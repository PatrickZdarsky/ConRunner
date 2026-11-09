package at.control.shiftcontrol.shift.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentPair;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;

public interface AssignmentSwitchRequestDao extends BasicDao<AssignmentSwitchRequest, Long> {

    Optional<AssignmentSwitchRequest> findBySlotsAndUsers(long offeredSlotId, String offeringUserId,
                                                          long requestedSlotId, String requestedUserId);

    void cancelTradesForPositionSlot(Long positionSlotId, String assignedUser);

    void cancelTradesForAssignment(Assignment assignment);

    Collection<AssignmentSwitchRequest> findOpenTradesForRequestedPositionAndOfferingUser(long positionSlotId, String userId);

    Collection<AssignmentSwitchRequest> findTradesForShiftPlanAndUser(long shiftPlanId, String userId);

    void cancelTradesForOfferedPositionAndRequestedUser(long positionSlotId, String userId);

    List<AssignmentSwitchRequest> findInverseTrade(AssignmentSwitchRequest trade);

    Collection<AssignmentSwitchRequest> findAllByAssignmentPairs(Collection<AssignmentPair> pairs);

    Collection<AssignmentSwitchRequest> findOpenTradesForOfferingPositionAndRequestedOwner(long positionSlotId, String userId);
}
