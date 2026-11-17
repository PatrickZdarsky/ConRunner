package at.conrunner.schedule.event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<CatalogEventEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from CatalogEventEntity event where event.id = :id")
    Optional<CatalogEventEntity> findForUpdate(UUID id);

    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "links")
    List<CatalogEventEntity> findAllByArchivedAtIsNullAndEndTimeGreaterThanEqualOrderByStartTimeAsc(Instant now);

    @EntityGraph(attributePaths = "links")
    List<CatalogEventEntity> findAllByArchivedAtIsNullAndNameContainingIgnoreCaseOrderByStartTimeAsc(String name);

    @EntityGraph(attributePaths = "links")
    List<CatalogEventEntity> findAllByArchivedAtIsNotNullAndNameContainingIgnoreCaseOrderByStartTimeAsc(String name);
}
