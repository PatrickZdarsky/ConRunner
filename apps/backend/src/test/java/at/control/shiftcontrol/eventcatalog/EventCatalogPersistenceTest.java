package at.control.shiftcontrol.eventcatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import at.control.shiftcontrol.TestcontainersConfiguration;
import at.control.shiftcontrol.eventcatalog.event.EventData;
import at.control.shiftcontrol.eventcatalog.event.EventDetails;
import at.control.shiftcontrol.eventcatalog.event.EventService;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogConflictException;
import at.control.shiftcontrol.eventcatalog.room.RoomData;
import at.control.shiftcontrol.eventcatalog.room.RoomDetails;
import at.control.shiftcontrol.eventcatalog.room.RoomService;
import at.control.shiftcontrol.eventcatalog.session.SessionData;
import at.control.shiftcontrol.eventcatalog.session.SessionDetails;
import at.control.shiftcontrol.eventcatalog.session.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({TestcontainersConfiguration.class, EventService.class, RoomService.class, SessionService.class})
class EventCatalogPersistenceTest {
    private static final Instant START = Instant.parse("2026-05-12T07:00:00Z");
    private static final Instant END = Instant.parse("2026-05-12T22:00:00Z");

    @Autowired
    private EventService eventService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAnEventRoomAndSessionWithStableUuidReferences() {
        EventDetails event = eventService.create(event("ShiftControl Fest", START, END));
        RoomDetails room = roomService.create(event.id(), new RoomData("Main Hall", "Center stage", "https://example.test/hall"));
        SessionDetails session = sessionService.create(event.id(), new SessionData("Opening ceremony", "Welcome",
            START.plusSeconds(3600), START.plusSeconds(7200), room.id()));

        assertEquals(event.id(), room.eventId());
        assertEquals(event.id(), session.eventId());
        assertEquals(room.id(), session.roomId());
    }

    @Test
    void deletingARoomKeepsTheLegacySetNullBehaviourForSessions() {
        EventDetails event = eventService.create(event("ShiftControl Fest", START, END));
        RoomDetails room = roomService.create(event.id(), new RoomData("Main Hall", null, null));
        SessionDetails session = sessionService.create(event.id(), new SessionData("Opening ceremony", null,
            START.plusSeconds(3600), START.plusSeconds(7200), room.id()));

        roomService.delete(room.id());
        entityManager.flush();
        entityManager.clear();

        assertNull(sessionService.get(session.id()).roomId());
    }

    @Test
    void enforcesCaseInsensitiveNamesWithinTheirEventButNotAcrossEvents() {
        EventDetails first = eventService.create(event("ShiftControl Fest", START, END));
        EventDetails second = eventService.create(event("Different Fest", START, END));
        roomService.create(first.id(), new RoomData("Main Hall", null, null));
        roomService.create(second.id(), new RoomData("Main Hall", null, null));

        assertThrows(EventCatalogConflictException.class,
            () -> roomService.create(first.id(), new RoomData("main hall", null, null)));
    }

    @Test
    void excludesEventsThatEndedBeforeTheRequestedOpenTime() {
        eventService.create(event("Past Fest", START.minusSeconds(86_400), START.minusSeconds(1)));
        eventService.create(event("Current Fest", START, END));

        List<EventDetails> open = eventService.findOpen(START);

        assertEquals(List.of("Current Fest"), open.stream().map(EventDetails::name).toList());
    }

    private static EventData event(String name, Instant start, Instant end) {
        return new EventData(name, null, null, start, end, null, List.of());
    }
}
