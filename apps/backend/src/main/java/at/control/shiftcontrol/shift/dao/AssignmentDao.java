package at.control.shiftcontrol.shift.dao;

import java.util.UUID;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.PositionSlot;

import lombok.NonNull;

public interface AssignmentDao extends BasicDao<Assignment, Long> {
    Optional<Assignment> findBySlotAndUser(long positionSlotId, UUID assignedUser);

    Collection<Assignment> findSignupRequestsByShiftPlanId(long shiftPlanId);

    Collection<Assignment> findAuctionsByShiftPlanId(long shiftPlanId);

    Collection<Assignment> findAuctionsByShiftPlanIdExcludingUser(long shiftPlanId, UUID userId);

    Collection<Assignment> getConflictingAssignments(UUID volunteerId, Instant startTime, Instant endTime);

    Collection<Assignment> getConflictingAssignments(UUID volunteerId, PositionSlot slot);

    Collection<Assignment> getActiveAssignmentsOfSlot(long positionSlotId);

    Collection<Assignment> getConflictingAssignmentsExcludingShift(UUID volunteerId, Instant startTime, Instant endTime, long shiftId);

    Collection<Assignment> getConflictingAssignmentsExcludingSlot(UUID volunteerId, Instant startTime, Instant endTime, long slotId);

    Collection<Assignment> getConflictingAssignmentsExcludingSlot(UUID volunteerId, PositionSlot slot);

    @NonNull Assignment getAssignmentForPositionSlotAndUser(long positionSlotId, UUID userId);

    Optional<Assignment> findAssignmentForPositionSlotAndUser(long positionSlotId, UUID userId);

    Collection<Assignment> getAssignmentForPositionSlotAndUsers(long positionSlotId, Collection<UUID> userIds);

    Collection<Assignment> findAssignmentsForShiftPlanAndUser(long shiftPlanId, UUID userId);

    Collection<Assignment> findActiveAssignmentsForShiftPlanAndUser(long shiftPlanId, UUID userId);

    void deleteAll(Collection<Assignment> ids);
}
