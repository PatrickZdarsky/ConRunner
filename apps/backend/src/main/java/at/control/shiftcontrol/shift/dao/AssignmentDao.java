package at.control.shiftcontrol.shift.dao;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import at.shiftcontrol.lib.entity.Assignment;
import at.shiftcontrol.lib.entity.PositionSlot;

import lombok.NonNull;

public interface AssignmentDao extends BasicDao<Assignment, Long> {
    Optional<Assignment> findBySlotAndUser(long positionSlotId, String assignedUser);

    Collection<Assignment> findSignupRequestsByShiftPlanId(long shiftPlanId);

    Collection<Assignment> findAuctionsByShiftPlanId(long shiftPlanId);

    Collection<Assignment> findAuctionsByShiftPlanIdExcludingUser(long shiftPlanId, String userId);

    Collection<Assignment> getConflictingAssignments(String volunteerId, Instant startTime, Instant endTime);

    Collection<Assignment> getConflictingAssignments(String volunteerId, PositionSlot slot);

    Collection<Assignment> getActiveAssignmentsOfSlot(long positionSlotId);

    Collection<Assignment> getConflictingAssignmentsExcludingShift(String volunteerId, Instant startTime, Instant endTime, long shiftId);

    Collection<Assignment> getConflictingAssignmentsExcludingSlot(String volunteerId, Instant startTime, Instant endTime, long slotId);

    Collection<Assignment> getConflictingAssignmentsExcludingSlot(String volunteerId, PositionSlot slot);

    @NonNull Assignment getAssignmentForPositionSlotAndUser(long positionSlotId, String userId);

    Optional<Assignment> findAssignmentForPositionSlotAndUser(long positionSlotId, String userId);

    Collection<Assignment> getAssignmentForPositionSlotAndUsers(long positionSlotId, Collection<String> userIds);

    Collection<Assignment> findAssignmentsForShiftPlanAndUser(long shiftPlanId, String userId);

    Collection<Assignment> findActiveAssignmentsForShiftPlanAndUser(long shiftPlanId, String userId);

    void deleteAll(Collection<Assignment> ids);
}
