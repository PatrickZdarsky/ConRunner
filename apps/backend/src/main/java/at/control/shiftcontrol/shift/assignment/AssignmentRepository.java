package at.control.shiftcontrol.shift.assignment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, UUID> {
    Optional<AssignmentEntity> findByPositionSlotIdAndUserId(UUID positionSlotId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select assignment from AssignmentEntity assignment
        where assignment.positionSlotId = :positionSlotId and assignment.userId = :userId
        """)
    Optional<AssignmentEntity> findByPositionSlotIdAndUserIdForUpdate(
        @Param("positionSlotId") UUID positionSlotId, @Param("userId") UUID userId);

    @Query("""
        select count(assignment) from AssignmentEntity assignment
        where assignment.positionSlotId = :positionSlotId and assignment.status in :statuses
        """)
    long countByPositionSlotIdAndStatusIn(@Param("positionSlotId") UUID positionSlotId,
                                          @Param("statuses") Collection<AssignmentStatus> statuses);

    @Query("""
        select (count(assignment) > 0) from AssignmentEntity assignment
        join PositionSlotEntity slot on slot.id = assignment.positionSlotId
        join ShiftEntity shift on shift.id = slot.shiftId
        where assignment.userId = :userId and assignment.status in :statuses
          and shift.startTime < :endTime and shift.endTime > :startTime
        """)
    boolean existsOccupiedAssignmentOverlappingShift(@Param("userId") UUID userId,
                                                      @Param("startTime") Instant startTime,
                                                      @Param("endTime") Instant endTime,
                                                      @Param("statuses") Collection<AssignmentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select assignment from AssignmentEntity assignment
        join PositionSlotEntity slot on slot.id = assignment.positionSlotId
        join ShiftEntity shift on shift.id = slot.shiftId
        join ShiftPlanEntity plan on plan.id = shift.planId
        where assignment.userId = :userId and plan.eventId = :eventId
          and assignment.status in :statuses
          and shift.startTime < :endTime and shift.endTime > :startTime
        order by assignment.id
        """)
    List<AssignmentEntity> findOccupiedOverlappingForUpdate(@Param("eventId") UUID eventId, @Param("userId") UUID userId,
                                                             @Param("startTime") Instant startTime, @Param("endTime") Instant endTime,
                                                             @Param("statuses") Collection<AssignmentStatus> statuses);

    @Query("""
        select assignment from AssignmentEntity assignment
        join PositionSlotEntity slot on slot.id = assignment.positionSlotId
        join ShiftEntity shift on shift.id = slot.shiftId
        where shift.planId = :planId and assignment.status = :status order by assignment.id
        """)
    List<AssignmentEntity> findAllByPlanIdAndStatus(@Param("planId") UUID planId,
                                                     @Param("status") AssignmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from AssignmentEntity assignment where assignment.id in :assignmentIds order by assignment.id")
    List<AssignmentEntity> findAllByIdInForUpdate(@Param("assignmentIds") Collection<UUID> assignmentIds);
}
