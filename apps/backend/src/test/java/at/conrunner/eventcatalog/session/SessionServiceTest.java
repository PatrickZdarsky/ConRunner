package at.conrunner.eventcatalog.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import at.conrunner.eventcatalog.audit.CatalogAudit;
import at.conrunner.eventcatalog.event.CatalogEventEntity;
import at.conrunner.eventcatalog.event.EventRepository;
import at.conrunner.eventcatalog.error.EventCatalogConflictException;
import at.conrunner.eventcatalog.error.EventCatalogValidationException;
import at.conrunner.eventcatalog.location.LocationEntity;
import at.conrunner.eventcatalog.location.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
    private static final Instant EVENT_START = Instant.parse("2026-05-12T07:00:00Z");
    private static final Instant EVENT_END = Instant.parse("2026-05-12T22:00:00Z");

    @Mock
    CatalogAudit audit;
    @Mock
    private EventRepository eventRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void create_preservesTheLegacyActivityTimeAndLocationRelationship() {
        UUID eventId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        CatalogEventEntity event = event(eventId);
        LocationEntity location = location(locationId, eventId, false);
        SessionData data = new SessionData("Opening ceremony", "Welcome", EVENT_START.plusSeconds(3600),
            EVENT_START.plusSeconds(7200), locationId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(sessionRepository.existsByEventIdAndNameIgnoreCase(eventId, "Opening ceremony")).thenReturn(false);
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(invocation -> {
            SessionEntity session = invocation.getArgument(0);
            session.setId(sessionId);
            return session;
        });

        SessionDetails result = sessionService.create(eventId, data);

        assertEquals(sessionId, result.id());
        assertEquals(eventId, result.eventId());
        assertEquals(locationId, result.locationId());
        assertEquals(data.startTime(), result.startTime());
    }

    @Test
    void create_rejectsATimeRangeOutsideTheEvent() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId)));
        SessionData data = new SessionData("Opening ceremony", null, EVENT_START.minusSeconds(1),
            EVENT_START.plusSeconds(3600), null);

        assertThrows(EventCatalogValidationException.class, () -> sessionService.create(eventId, data));
    }

    @Test
    void create_rejectsALocationFromAnotherEvent() {
        UUID eventId = UUID.randomUUID();
        UUID otherEventId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId)));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location(locationId, otherEventId, false)));
        SessionData data = new SessionData("Opening ceremony", null, EVENT_START.plusSeconds(1),
            EVENT_START.plusSeconds(3600), locationId);

        assertThrows(EventCatalogValidationException.class, () -> sessionService.create(eventId, data));
    }

    @Test
    void create_allowsAReadOnlyImportedLocationFromTheSameEvent() {
        UUID eventId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId)));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location(locationId, eventId, true)));
        when(sessionRepository.existsByEventIdAndNameIgnoreCase(eventId, "Opening ceremony")).thenReturn(false);
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SessionData data = new SessionData("Opening ceremony", null, EVENT_START.plusSeconds(1),
            EVENT_START.plusSeconds(3600), locationId);

        SessionDetails result = sessionService.create(eventId, data);

        assertEquals(locationId, result.locationId());
    }

    @Test
    void create_rejectsDuplicateSessionNamesWithinAnEventIgnoringCase() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId)));
        when(sessionRepository.existsByEventIdAndNameIgnoreCase(eventId, "opening ceremony")).thenReturn(true);
        SessionData data = new SessionData("opening ceremony", null, EVENT_START.plusSeconds(1),
            EVENT_START.plusSeconds(3600), null);

        assertThrows(EventCatalogConflictException.class, () -> sessionService.create(eventId, data));
    }

    @Test
    void suggest_matchesTitleAndOverlappingTimeRangeIncludingTolerance() {
        UUID eventId = UUID.randomUUID();
        SessionEntity matching = session(UUID.randomUUID(), eventId, "Opening ceremony", EVENT_START.plusSeconds(3600),
            EVENT_START.plusSeconds(7200));
        SessionEntity outsideTolerance = session(UUID.randomUUID(), eventId, "Opening discussion", EVENT_START.plusSeconds(12 * 3600),
            EVENT_START.plusSeconds(13 * 3600));
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(sessionRepository.findAllByEventIdOrderByStartTimeAsc(eventId)).thenReturn(List.of(matching, outsideTolerance));

        List<SessionDetails> result = sessionService.suggest(eventId,
            new SessionSuggestion(" opening ", EVENT_START.plusSeconds(2 * 3600), EVENT_START.plusSeconds(3 * 3600), 30));

        assertEquals(List.of(matching.getId()), result.stream().map(SessionDetails::id).toList());
    }

    @Test
    void suggestion_rejectsANegativeTolerance() {
        assertThrows(EventCatalogValidationException.class,
            () -> new SessionSuggestion(null, EVENT_START, EVENT_START.plusSeconds(1), -1));
    }

    private static CatalogEventEntity event(UUID id) {
        CatalogEventEntity event = new CatalogEventEntity();
        event.setId(id);
        event.setStartTime(EVENT_START);
        event.setEndTime(EVENT_END);
        return event;
    }

    private static LocationEntity location(UUID id, UUID eventId, boolean readOnly) {
        LocationEntity location = new LocationEntity();
        location.setId(id);
        location.setEventId(eventId);
        location.setReadOnly(readOnly);
        return location;
    }

    private static SessionEntity session(UUID id, UUID eventId, String title, Instant start, Instant end) {
        SessionEntity session = new SessionEntity();
        session.setId(id);
        session.setEventId(eventId);
        session.setName(title);
        session.setStartTime(start);
        session.setEndTime(end);
        return session;
    }
}
