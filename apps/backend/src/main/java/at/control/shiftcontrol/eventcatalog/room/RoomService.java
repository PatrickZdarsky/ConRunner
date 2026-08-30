package at.control.shiftcontrol.eventcatalog.room;

import java.util.List;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.event.EventRepository;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogConflictException;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogNotFoundException;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {
    private final EventRepository eventRepository;
    private final RoomRepository roomRepository;

    public RoomService(EventRepository eventRepository, RoomRepository roomRepository) {
        this.eventRepository = eventRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public RoomDetails create(UUID eventId, RoomData data) {
        requireEvent(eventId);
        if (data == null) {
            throw new EventCatalogValidationException("Room data is required");
        }
        assertNameAvailable(eventId, data.name(), null);

        RoomEntity room = new RoomEntity();
        room.setEventId(eventId);
        room.setReadOnly(false);
        apply(room, data);
        return toDetails(roomRepository.save(room));
    }

    @Transactional
    public RoomDetails update(UUID roomId, RoomData data) {
        RoomEntity room = getEntity(roomId);
        assertMutable(room);
        if (data == null) {
            throw new EventCatalogValidationException("Room data is required");
        }
        assertNameAvailable(room.getEventId(), data.name(), room);
        apply(room, data);
        return toDetails(roomRepository.save(room));
    }

    public RoomDetails get(UUID roomId) {
        return toDetails(getEntity(roomId));
    }

    public List<RoomDetails> findAllForEvent(UUID eventId) {
        requireEvent(eventId);
        return roomRepository.findAllByEventIdOrderByNameAsc(eventId).stream().map(RoomService::toDetails).toList();
    }

    @Transactional
    public void delete(UUID roomId) {
        RoomEntity room = getEntity(roomId);
        assertMutable(room);
        roomRepository.delete(room);
    }

    private void requireEvent(UUID eventId) {
        if (eventId == null || !eventRepository.existsById(eventId)) {
            throw new EventCatalogNotFoundException("Event not found: " + eventId);
        }
    }

    private RoomEntity getEntity(UUID roomId) {
        if (roomId == null) {
            throw new EventCatalogValidationException("Room ID is required");
        }
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new EventCatalogNotFoundException("Room not found: " + roomId));
    }

    private void assertNameAvailable(UUID eventId, String name, RoomEntity currentRoom) {
        if (currentRoom != null && currentRoom.getName().equalsIgnoreCase(name)) {
            return;
        }
        if (roomRepository.existsByEventIdAndNameIgnoreCase(eventId, name)) {
            throw new EventCatalogConflictException("Room name must be unique within an event");
        }
    }

    private static void apply(RoomEntity room, RoomData data) {
        room.setName(data.name());
        room.setDescription(data.description());
        room.setUrl(data.url());
    }

    private static void assertMutable(RoomEntity room) {
        if (room.isReadOnly()) {
            throw new EventCatalogConflictException("Cannot modify a read-only imported room");
        }
    }

    private static RoomDetails toDetails(RoomEntity room) {
        return new RoomDetails(room.getId(), room.getEventId(), room.getName(), room.getDescription(), room.getUrl(), room.isReadOnly());
    }

}
