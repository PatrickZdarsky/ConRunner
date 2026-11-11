package at.control.shiftcontrol.shift.dao;

import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentPair;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;

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
