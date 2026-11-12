package at.conrunner.eventcatalog.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<CatalogEventEntity, UUID> {
    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "links")
    List<CatalogEventEntity> findAllByArchivedAtIsNullAndEndTimeGreaterThanEqualOrderByStartTimeAsc(Instant now);

    @EntityGraph(attributePaths = "links")
    List<CatalogEventEntity> findAllByArchivedAtIsNullAndNameContainingIgnoreCaseOrderByStartTimeAsc(String name);

    @EntityGraph(attributePaths = "links")
    List<CatalogEventEntity> findAllByArchivedAtIsNotNullAndNameContainingIgnoreCaseOrderByStartTimeAsc(String name);
}
