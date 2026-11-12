package at.conrunner.shift.dao;

import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.AssignmentPair;
import at.conrunner.shift.entity.AssignmentSwitchRequest;

public interface AssignmentSwitchRequestDao extends BasicDao<AssignmentSwitchRequest, Long> {

    Optional<AssignmentSwitchRequest> findBySlotsAndUsers(long offeredSlotId, UUID offeringUserId,
                                                          long requestedSlotId, UUID requestedUserId);

    void cancelTradesForPositionSlot(Long positionSlotId, UUID assignedUser);

    void cancelTradesForAssignment(Assignment assignment);

    Collection<AssignmentSwitchRequest> findOpenTradesForRequestedPositionAndOfferingUser(long positionSlotId, UUID userId);

    Collection<AssignmentSwitchRequest> findTradesForShiftPlanAndUser(long shiftPlanId, UUID userId);

    void cancelTradesForOfferedPositionAndRequestedUser(long positionSlotId, UUID userId);

    List<AssignmentSwitchRequest> findInverseTrade(AssignmentSwitchRequest trade);

    Collection<AssignmentSwitchRequest> findAllByAssignmentPairs(Collection<AssignmentPair> pairs);

    Collection<AssignmentSwitchRequest> findOpenTradesForOfferingPositionAndRequestedOwner(long positionSlotId, UUID userId);
}
