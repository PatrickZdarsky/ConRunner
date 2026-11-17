package at.conrunner.schedule.location;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
    List<LocationEntity> findAllByEventIdAndRetiredFalseOrderBySortPositionAscNameAscIdAsc(UUID eventId);

    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    List<LocationEntity> findAllByEventIdOrderByNameAsc(UUID eventId);
}
