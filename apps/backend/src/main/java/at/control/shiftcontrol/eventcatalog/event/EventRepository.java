package at.control.shiftcontrol.eventcatalog.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<CatalogEventEntity, UUID> {
    boolean existsByNameIgnoreCase(String name);

    List<CatalogEventEntity> findAllByEndTimeGreaterThanEqualOrderByStartTimeAsc(Instant now);

    List<CatalogEventEntity> findAllByNameContainingIgnoreCaseOrderByStartTimeAsc(String name);
}
