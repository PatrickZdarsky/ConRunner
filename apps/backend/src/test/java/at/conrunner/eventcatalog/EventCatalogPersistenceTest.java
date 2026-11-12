package at.conrunner.eventcatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import at.conrunner.eventcatalog.audit.CatalogAudit;
import at.conrunner.TestcontainersConfiguration;
import at.conrunner.eventcatalog.event.EventData;
import at.conrunner.eventcatalog.event.EventDetails;
import at.conrunner.eventcatalog.event.EventService;
import at.conrunner.eventcatalog.error.EventCatalogConflictException;
import at.conrunner.eventcatalog.location.LocationData;
import at.conrunner.eventcatalog.location.LocationDetails;
import at.conrunner.eventcatalog.location.LocationService;
import at.conrunner.eventcatalog.session.SessionData;
import at.conrunner.eventcatalog.session.SessionDetails;
import at.conrunner.eventcatalog.session.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({TestcontainersConfiguration.class, EventService.class, LocationService.class, SessionService.class})
class EventCatalogPersistenceTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    CatalogAudit audit;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    Clock clock;

    private static final Instant START = Instant.parse("2026-05-12T07:00:00Z");
    private static final Instant END = Instant.parse("2026-05-12T22:00:00Z");

    @Autowired
    private EventService eventService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAnEventLocationAndSessionWithStableUuidReferences() {
        EventDetails event = eventService.create(event("ShiftControl Fest", START, END));
        LocationDetails location = locationService.create(event.id(), new LocationData("Main Hall", "Center stage", "https://example.test/hall"));
        SessionDetails session = sessionService.create(event.id(), new SessionData("Opening ceremony", "Welcome",
            START.plusSeconds(3600), START.plusSeconds(7200), location.id()));

        assertEquals(event.id(), location.eventId());
        assertEquals(event.id(), session.eventId());
        assertEquals(location.id(), session.locationId());
    }

    @Test
    void deletingALocationKeepsTheLegacySetNullBehaviourForSessions() {
        EventDetails event = eventService.create(event("ShiftControl Fest", START, END));
        LocationDetails location = locationService.create(event.id(), new LocationData("Main Hall", null, null));
        SessionDetails session = sessionService.create(event.id(), new SessionData("Opening ceremony", null,
            START.plusSeconds(3600), START.plusSeconds(7200), location.id()));

        locationService.delete(location.id());
        entityManager.flush();
        entityManager.clear();

        assertNull(sessionService.get(session.id()).locationId());
    }

    @Test
    void enforcesCaseInsensitiveNamesWithinTheirEventButNotAcrossEvents() {
        EventDetails first = eventService.create(event("ShiftControl Fest", START, END));
        EventDetails second = eventService.create(event("Different Fest", START, END));
        locationService.create(first.id(), new LocationData("Main Hall", null, null));
        locationService.create(second.id(), new LocationData("Main Hall", null, null));

        assertThrows(EventCatalogConflictException.class,
            () -> locationService.create(first.id(), new LocationData("main hall", null, null)));
    }

    @Test
    void excludesEventsThatEndedBeforeTheRequestedOpenTime() {
        eventService.create(event("Past Fest", START.minusSeconds(86_400), START.minusSeconds(1)));
        eventService.create(event("Current Fest", START, END));

        List<EventDetails> open = eventService.findOpen(START);

        assertEquals(List.of("Current Fest"), open.stream().map(EventDetails::name).toList());
    }

    @Test
    void archivesAnEventWithoutRemovingItsCatalogData() {
        org.mockito.Mockito.when(clock.instant()).thenReturn(END.plusSeconds(1));
        EventDetails event = eventService.create(event("Past Fest", START, END));
        LocationDetails location = locationService.create(event.id(), new LocationData("Main Hall", null, null));
        SessionDetails session = sessionService.create(event.id(),
            new SessionData("Opening", null, START, START.plusSeconds(3600), location.id()));
        eventService.archive(event.id());
        entityManager.flush();
        entityManager.clear();

        assertEquals(List.of(), eventService.search(null));
        assertEquals(List.of(), eventService.findOpen(START));
        assertEquals(List.of(event.id()), eventService.search("fest", true).stream().map(EventDetails::id).toList());
        assertEquals(location.id(), sessionService.get(session.id()).locationId());

        eventService.unarchive(event.id());
        assertEquals(List.of(event.id()), eventService.search(null).stream().map(EventDetails::id).toList());
    }

    private static EventData event(String name, Instant start, Instant end) {
        return new EventData(name, null, null, start, end, null, List.of());
    }
}
