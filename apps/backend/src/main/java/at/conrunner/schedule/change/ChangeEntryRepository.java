package at.conrunner.schedule.change;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChangeEntryRepository extends JpaRepository<ChangeEntryEntity, UUID> {
    List<ChangeEntryEntity> findAllByEventIdAndBatchIdOrderByEntryOrderAsc(UUID eventId, UUID batchId);

    List<ChangeEntryEntity> findAllByEventIdAndBatchIdAndStateOrderByEntryOrderAsc(
            UUID eventId, UUID batchId, ChangeState state);

    @Query("""
        select entry from ChangeEntryEntity entry
        join ChangeBatchEntity batch on batch.id = entry.batchId and batch.eventId = entry.eventId
        where entry.eventId = :eventId and entry.occurrenceId = :occurrenceId
          and entry.state = at.conrunner.schedule.change.ChangeState.COMMITTED
          and batch.state = at.conrunner.schedule.change.ChangeState.COMMITTED
        order by batch.sequence, entry.entryOrder
        """)
    List<ChangeEntryEntity> findCommittedOccurrenceHistory(UUID eventId, UUID occurrenceId);

    @Query("""
        select entry from ChangeEntryEntity entry
        join ChangeBatchEntity batch on batch.id = entry.batchId and batch.eventId = entry.eventId
        where entry.eventId = :eventId and entry.locationId = :locationId
          and entry.state = at.conrunner.schedule.change.ChangeState.COMMITTED
          and batch.state = at.conrunner.schedule.change.ChangeState.COMMITTED
        order by batch.sequence, entry.entryOrder
        """)
    List<ChangeEntryEntity> findCommittedLocationHistory(UUID eventId, UUID locationId, Pageable page);
}
