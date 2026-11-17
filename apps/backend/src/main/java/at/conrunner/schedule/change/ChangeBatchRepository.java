package at.conrunner.schedule.change;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ChangeBatchRepository extends JpaRepository<ChangeBatchEntity, UUID> {
    @Query("""
        select batch from ChangeBatchEntity batch
        where batch.eventId = :eventId and batch.state = at.conrunner.schedule.change.ChangeState.DRAFT
        """)
    Optional<ChangeBatchEntity> findCurrentDraft(UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select batch from ChangeBatchEntity batch where batch.eventId = :eventId and batch.id = :id")
    Optional<ChangeBatchEntity> findForUpdate(UUID eventId, UUID id);

    Optional<ChangeBatchEntity> findByEventIdAndIdempotencyKey(UUID eventId, String idempotencyKey);

    @Query("""
        select coalesce(max(batch.sequence), 0)
        from ChangeBatchEntity batch
        where batch.eventId = :eventId and batch.state = at.conrunner.schedule.change.ChangeState.COMMITTED
        """)
    long latestCommittedSequence(UUID eventId);

    @Query("""
        select batch from ChangeBatchEntity batch
        where batch.eventId = :eventId and batch.state = at.conrunner.schedule.change.ChangeState.COMMITTED
          and batch.sequence > :afterSequence
        order by batch.sequence
        """)
    List<ChangeBatchEntity> findCommittedChangesAfter(UUID eventId, long afterSequence, Pageable page);
}
