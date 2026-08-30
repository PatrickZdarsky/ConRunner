package at.control.shiftcontrol.eventcatalog.room;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {
    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    List<RoomEntity> findAllByEventIdOrderByNameAsc(UUID eventId);
}
