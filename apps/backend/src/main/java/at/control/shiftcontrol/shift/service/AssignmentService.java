package at.control.shiftcontrol.shift.service;

import java.util.Collection;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotRequestDto;

import lombok.NonNull;

public interface AssignmentService {
    /**
     * reassigns the auction to the given volunteer.
     * updates reward points & publishes event
     *
     * @param auction to claim
     * @param newVolunteer to be assigned to the auction
     * @param requestDto contains reward points hash
     * @return reassigned and accepted auction
     */
    @NonNull Assignment claimAuction(@NonNull Assignment auction, @NonNull Volunteer newVolunteer, @NonNull PositionSlotRequestDto requestDto);

    /**
     * swapps the volunteers of the given switch request.
     * cancels all involved trades of the offering and requested assignment
     * deletes inverse trade if present
     * updates reward points & publishes event
     *
     * @param oldTrade trade to execute
     * @return executed trade, where volunteers are swapped
     */
    @NonNull AssignmentSwitchRequest executeTrade(@NonNull AssignmentSwitchRequest oldTrade);

    void cancelOtherTrades(@NonNull AssignmentSwitchRequest trade);

    /**
     * accepts an already existing assignment.
     * assumes that eligibility, conflicts, etc. have already been checked
     * updates reward points & publishes event
     *
     * @param assignment to accept
     * @return the accepted assignment
     */
    @NonNull Assignment accept(@NonNull Assignment assignment);

    /**
     * assigns the volunteer to the position slot.
     * assumes that eligibility, conflicts, etc. have already been checked
     * cancels trades where this slot was offered to the volunteer
     * updates reward points & publishes event
     *
     * @param positionSlot to assign the volunteer to
     * @param volunteer to assign to the position slot
     * @return the newly created assignment
     */
    @NonNull Assignment assign(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer, @NonNull PositionSlotRequestDto requestDto);

    /**
     * <b>NO VALIDATION!</b><br/>
     * unassigns the volunteer from the position slot.
     * deletes dependent trades and the assignment itself in the process
     * updates reward points & publishes event
     *
     * @param assignment to dissolve
     */
    void unassignInternal(@NonNull Assignment assignment);

    /**
     * unassign all volunteers from auctions and declines join requests for a given shift plan.
     * deletes all involved trades in the process
     * updates reward points & publishes event
     *
     * @param shiftPlan to unassign all auctions
     */
    void unassignAllAuctions(@NonNull ShiftPlan shiftPlan);

    /**
     * declines all signuprequests for a given shiftplan.
     *
     * @param shiftPlan to decline all signup requests
     */
    void declineAllSignupRequests(@NonNull ShiftPlan shiftPlan);

    /**
     * get all assignments for a user
     *
     * @param plan to fetch the assignments from
     * @param volunteer to assignments belong to
     * @return all assignments of the user in the given shift plan
     */
    @NonNull Collection<Assignment> getAllAssignmentsForUser(@NonNull ShiftPlan plan, @NonNull Volunteer volunteer);
}
