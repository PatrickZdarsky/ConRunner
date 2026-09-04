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

public interface StaffingAssignmentRepository extends JpaRepository<StaffingAssignmentEntity, UUID> {
    Optional<StaffingAssignmentEntity> findByPositionSlotIdAndUserId(UUID positionSlotId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select assignment from StaffingAssignmentEntity assignment
        where assignment.positionSlotId = :positionSlotId and assignment.userId = :userId
        """)
    Optional<StaffingAssignmentEntity> findByPositionSlotIdAndUserIdForUpdate(
        @Param("positionSlotId") UUID positionSlotId, @Param("userId") UUID userId);

    @Query("""
        select count(assignment) from StaffingAssignmentEntity assignment
        where assignment.positionSlotId = :positionSlotId and assignment.state in :states
        """)
    long countByPositionSlotIdAndStateIn(@Param("positionSlotId") UUID positionSlotId,
                                         @Param("states") Collection<AssignmentState> states);

    @Query("""
        select (count(assignment) > 0)
        from StaffingAssignmentEntity assignment
        join PositionSlotEntity slot on slot.id = assignment.positionSlotId
        join StaffingShiftEntity staffingShift on staffingShift.id = slot.staffingShiftId
        where assignment.userId = :userId
          and assignment.state in :states
          and staffingShift.startTime < :endTime
          and staffingShift.endTime > :startTime
        """)
    boolean existsOccupiedAssignmentOverlappingShift(@Param("userId") UUID userId,
                                                      @Param("startTime") Instant startTime,
                                                      @Param("endTime") Instant endTime,
                                                      @Param("states") Collection<AssignmentState> states);

    @Query("""
        select (count(assignment) > 0)
        from StaffingAssignmentEntity assignment
        join PositionSlotEntity slot on slot.id = assignment.positionSlotId
        join StaffingShiftEntity staffingShift on staffingShift.id = slot.staffingShiftId
        join ShiftPlanEntity plan on plan.id = staffingShift.planId
        where assignment.userId = :userId
          and plan.eventId = :eventId
          and assignment.state in ('ACCEPTED', 'LEAVE_REQUESTED')
          and staffingShift.startTime < :endTime
          and staffingShift.endTime > :startTime
        """)
    boolean existsOccupiedAssignmentOverlapping(@Param("eventId") UUID eventId, @Param("userId") UUID userId,
                                                @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select assignment from StaffingAssignmentEntity assignment
        join PositionSlotEntity slot on slot.id = assignment.positionSlotId
        join StaffingShiftEntity staffingShift on staffingShift.id = slot.staffingShiftId
        where staffingShift.planId = :planId and assignment.state = :state
        order by assignment.id
        """)
    List<StaffingAssignmentEntity> findAllByPlanIdAndStateForUpdate(@Param("planId") UUID planId,
                                                                     @Param("state") AssignmentState state);
}
