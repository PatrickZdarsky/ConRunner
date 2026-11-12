package at.conrunner.eventcatalog.location;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    List<LocationEntity> findAllByEventIdOrderByNameAsc(UUID eventId);
}
