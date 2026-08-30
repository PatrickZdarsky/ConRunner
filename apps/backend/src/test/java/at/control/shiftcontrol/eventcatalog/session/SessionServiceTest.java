package at.control.shiftcontrol.eventcatalog.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.event.CatalogEventEntity;
import at.control.shiftcontrol.eventcatalog.event.EventRepository;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogConflictException;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogValidationException;
import at.control.shiftcontrol.eventcatalog.room.RoomEntity;
import at.control.shiftcontrol.eventcatalog.room.RoomRepository;
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
    private EventRepository eventRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void create_preservesTheLegacyActivityTimeAndRoomRelationship() {
        UUID eventId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        CatalogEventEntity event = event(eventId);
        RoomEntity room = room(roomId, eventId, false);
        SessionData data = new SessionData("Opening ceremony", "Welcome", EVENT_START.plusSeconds(3600),
            EVENT_START.plusSeconds(7200), roomId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(sessionRepository.existsByEventIdAndNameIgnoreCase(eventId, "Opening ceremony")).thenReturn(false);
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(invocation -> {
            SessionEntity session = invocation.getArgument(0);
            session.setId(sessionId);
            return session;
        });

        SessionDetails result = sessionService.create(eventId, data);

        assertEquals(sessionId, result.id());
        assertEquals(eventId, result.eventId());
        assertEquals(roomId, result.roomId());
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
    void create_rejectsARoomFromAnotherEvent() {
        UUID eventId = UUID.randomUUID();
        UUID otherEventId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId)));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room(roomId, otherEventId, false)));
        SessionData data = new SessionData("Opening ceremony", null, EVENT_START.plusSeconds(1),
            EVENT_START.plusSeconds(3600), roomId);

        assertThrows(EventCatalogValidationException.class, () -> sessionService.create(eventId, data));
    }

    @Test
    void create_allowsAReadOnlyImportedRoomFromTheSameEvent() {
        UUID eventId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId)));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room(roomId, eventId, true)));
        when(sessionRepository.existsByEventIdAndNameIgnoreCase(eventId, "Opening ceremony")).thenReturn(false);
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SessionData data = new SessionData("Opening ceremony", null, EVENT_START.plusSeconds(1),
            EVENT_START.plusSeconds(3600), roomId);

        SessionDetails result = sessionService.create(eventId, data);

        assertEquals(roomId, result.roomId());
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

    private static RoomEntity room(UUID id, UUID eventId, boolean readOnly) {
        RoomEntity room = new RoomEntity();
        room.setId(id);
        room.setEventId(eventId);
        room.setReadOnly(readOnly);
        return room;
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
