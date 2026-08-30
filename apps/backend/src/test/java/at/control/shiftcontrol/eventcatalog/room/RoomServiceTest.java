package at.control.shiftcontrol.eventcatalog.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.event.EventRepository;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    void create_createsAMutableRoomForAnExistingEvent() {
        UUID eventId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(roomRepository.existsByEventIdAndNameIgnoreCase(eventId, "Main Hall")).thenReturn(false);
        when(roomRepository.save(any(RoomEntity.class))).thenAnswer(invocation -> {
            RoomEntity room = invocation.getArgument(0);
            room.setId(roomId);
            return room;
        });

        RoomDetails result = roomService.create(eventId, new RoomData(" Main Hall ", "Center stage", "https://example.test/hall"));

        ArgumentCaptor<RoomEntity> roomCaptor = ArgumentCaptor.forClass(RoomEntity.class);
        verify(roomRepository).save(roomCaptor.capture());
        RoomEntity saved = roomCaptor.getValue();
        assertEquals(eventId, saved.getEventId());
        assertEquals("Main Hall", saved.getName());
        assertFalse(saved.isReadOnly());
        assertEquals(roomId, result.id());
    }

    @Test
    void create_rejectsADuplicateRoomNameWithinTheSameEventIgnoringCase() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(roomRepository.existsByEventIdAndNameIgnoreCase(eventId, "main hall")).thenReturn(true);

        assertThrows(EventCatalogConflictException.class,
            () -> roomService.create(eventId, new RoomData("main hall", null, null)));
    }

    @Test
    void update_andDelete_rejectReadOnlyImportedRooms() {
        UUID roomId = UUID.randomUUID();
        RoomEntity importedRoom = new RoomEntity();
        importedRoom.setId(roomId);
        importedRoom.setEventId(UUID.randomUUID());
        importedRoom.setName("Imported room");
        importedRoom.setReadOnly(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(importedRoom));

        assertThrows(EventCatalogConflictException.class,
            () -> roomService.update(roomId, new RoomData("Updated", null, null)));
        assertThrows(EventCatalogConflictException.class, () -> roomService.delete(roomId));
    }

    @Test
    void get_returnsTheStoredRoomWithoutExposingItsEntity() {
        UUID roomId = UUID.randomUUID();
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setEventId(UUID.randomUUID());
        room.setName("Main Hall");
        room.setDescription("Center stage");
        room.setUrl("https://example.test/hall");
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        RoomDetails result = roomService.get(roomId);

        assertEquals(roomId, result.id());
        assertEquals("Main Hall", result.name());
    }
}
