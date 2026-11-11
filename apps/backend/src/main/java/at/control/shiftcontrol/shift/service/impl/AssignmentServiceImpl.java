package at.control.shiftcontrol.shift.service.impl;

import at.control.shiftcontrol.shift.audit.AssignmentAudit;
import at.control.shiftcontrol.shift.audit.OperationsAudit;
import at.control.shiftcontrol.communications.api.NotificationTypes;
import at.control.shiftcontrol.communications.api.NotificationContext;

import java.util.Collection;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import at.control.shiftcontrol.communications.api.NotificationRequest;
import at.control.shiftcontrol.communications.api.NotificationRequests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.event.events.AssignmentSwitchEvent;
import at.control.shiftcontrol.shift.lib.event.events.ClaimedAuctionEvent;
import at.control.shiftcontrol.shift.lib.event.events.PositionSlotVolunteerEvent;
import at.control.shiftcontrol.platform.exception.IllegalArgumentException;
import at.control.shiftcontrol.platform.exception.IllegalStateException;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.type.TradeStatus;
import at.control.shiftcontrol.shift.dao.AssignmentDao;
import at.control.shiftcontrol.shift.dao.AssignmentSwitchRequestDao;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotRequestDto;
import at.control.shiftcontrol.shift.mapper.AssignmentAssemblingMapper;
import at.control.shiftcontrol.shift.mapper.TradeMapper;
import at.control.shiftcontrol.shift.service.AssignmentService;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsService;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {
    private final AssignmentAudit audit;
    private final OperationsAudit operationsAudit;
    private final RewardPointsService rewardPointsService;
    private final AssignmentDao assignmentDao;
    private final AssignmentSwitchRequestDao assignmentSwitchRequestDao;
    private final ApplicationEventPublisher publisher;
    private final NotificationRequests notificationRequests;

    @Override
    @Transactional
    public @NonNull Assignment claimAuction(@NonNull Assignment auction, @NonNull Volunteer newVolunteer, @NonNull PositionSlotRequestDto requestDto) {
        var before = audit.snapshot(auction);
        var oldVolunteerId = auction.getAssignedVolunteer().getId();
        Assignment oldAuction = AssignmentAssemblingMapper.shallowCopy(auction);
        // execute auction
        auction = reassign(auction, newVolunteer);

        // update reward points
        rewardPointsService.onAssignmentReassignedAuction(oldAuction, auction, requestDto.getAcceptedRewardPointsConfigHash());
        audit.auctionClaimed(auction, before);

        notificationRequests.request(new NotificationRequest(java.util.UUID.randomUUID(), oldVolunteerId,
            NotificationTypes.AUCTION_CLAIMED, NotificationContext.encode(String.valueOf(auction.getPositionSlot().getShift().getShiftPlan().getEvent().getId()), auction.getPositionSlot().getName(), null)));

        publisher.publishEvent(ClaimedAuctionEvent.auctionClaimed(auction, oldAuction, oldVolunteerId.toString()));
        return auction;
    }

    @Override
    @Transactional
    public @NonNull AssignmentSwitchRequest executeTrade(@NonNull AssignmentSwitchRequest trade) {
        // delete inverse trade if exists
        List<AssignmentSwitchRequest> inverse = assignmentSwitchRequestDao.findInverseTrade(trade)
            .stream()
            .filter(x -> x.getStatus().equals(TradeStatus.OPEN))
            .toList();
        if (inverse.size() > 1) {
            throw new IllegalStateException("more than one inverse trade is open" + inverse);
        }
        if (!inverse.isEmpty()) {
            operationsAudit.tradeDeleted(inverse.get(0));
            assignmentSwitchRequestDao.delete(inverse.get(0));
        }
        // cancel all trades for involved assignments
        cancelOtherTrades(trade);

        // update fields and id of assignments
        var offeringBefore = audit.snapshot(trade.getOfferingAssignment());
        var requestedBefore = audit.snapshot(trade.getRequestedAssignment());
        AssignmentSwitchRequest oldTrade = TradeMapper.shallowCopy(trade);
        reassign(trade.getOfferingAssignment(), oldTrade.getRequestedAssignment().getAssignedVolunteer());
        reassign(trade.getRequestedAssignment(), oldTrade.getOfferingAssignment().getAssignedVolunteer());
        trade.setStatus(TradeStatus.ACCEPTED);
        assignmentSwitchRequestDao.save(trade);


        rewardPointsService.onAssignmentReassignedTrade(trade.getOfferingAssignment(), trade.getRequestedAssignment());
        audit.traded(trade.getOfferingAssignment(), offeringBefore);
        audit.traded(trade.getRequestedAssignment(), requestedBefore);

        publisher.publishEvent(AssignmentSwitchEvent.assignmentSwitched(oldTrade.getRequestedAssignment(), oldTrade.getOfferingAssignment()));

        return trade;
    }

    @Override
    public void cancelOtherTrades(@NonNull AssignmentSwitchRequest trade) {
        // this trade does not need to be excluded because it will be set to ACCEPTED in the next step
        assignmentSwitchRequestDao.cancelTradesForAssignment(trade.getRequestedAssignment());
        assignmentSwitchRequestDao.cancelTradesForAssignment(trade.getOfferingAssignment());
    }

    /**
     * reassigns the assignment to the given volunteer.
     * handles all dependencies of the assignment, since reassigning results in a new primary key
     * deletes the old assignment and persists a new one in the process
     *
     * @param oldAssignment the assignment containing the old assigned volunteer
     * @param newVolunteer volunteer to replace the old volunteer
     * @return new assignment where the given volunteer is assigned
     */
    private Assignment reassign(Assignment oldAssignment, Volunteer newVolunteer) {
        oldAssignment.setAssignedVolunteer(newVolunteer);
        oldAssignment.setStatus(AssignmentStatus.ACCEPTED);
        oldAssignment = assignmentDao.save(oldAssignment);
        return oldAssignment;
    }

    @Override
    @Transactional
    public @NonNull Assignment accept(@NonNull Assignment assignment) {
        if (assignment.getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT) {
            throw new IllegalArgumentException("Assignment must be an assignment request");
        }

        var before = audit.snapshot(assignment);
        // update reward points
        rewardPointsService.onAssignmentAccepted(assignment);

        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment = assignmentDao.save(assignment);

        audit.accepted(assignment, before);
        publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotJoined(assignment.getPositionSlot(), assignment.getAssignedVolunteer().getId().toString()));
        return assignment;
    }

    @Override
    @Transactional
    public @NonNull Assignment assign(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer, @NonNull PositionSlotRequestDto requestDto) {
        // create assignment
        Assignment assignment = Assignment.of(positionSlot, volunteer, AssignmentStatus.ACCEPTED);

        // update reward points
        rewardPointsService.onAssignmentCreated(
            assignment,
            requestDto.getAcceptedRewardPointsConfigHash());

        // close trades where this slot was offered to current user
        assignmentSwitchRequestDao.cancelTradesForOfferedPositionAndRequestedUser(positionSlot.getId(), volunteer.getId());
        assignment = assignmentDao.save(assignment);


        audit.joined(assignment);
        publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotJoined(assignment.getPositionSlot(), volunteer.getId().toString()));
        return assignment;
    }

    @Override
    @Transactional
    public void unassignInternal(@NonNull Assignment assignment) {
        var before = audit.snapshot(assignment);
        //ACT: update reward points
        rewardPointsService.onAssignmentRemoved(
            assignment
        );

        //ACT: cancel trades & remove assignment
        assignmentSwitchRequestDao.cancelTradesForAssignment(assignment);
        assignmentDao.delete(assignment);

        audit.left(assignment, before);
        //NOTIFY: publish event
        publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotLeft(assignment.getPositionSlot(), assignment.getAssignedVolunteer().getId().toString()));
    }

    @Override
    @Transactional
    public void unassignAllAuctions(@NonNull ShiftPlan shiftPlan) {
        Collection<Assignment> auctions = assignmentDao.findAuctionsByShiftPlanId(shiftPlan.getId());

        auctions.forEach(auction -> {
            var before = audit.snapshot(auction);
            // update reward points
            rewardPointsService.onAssignmentRemoved(
                auction
            );

            audit.auctionClosed(auction, before);
            // publish event
            publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotLeft(auction.getPositionSlot(), auction.getAssignedVolunteer().getId().toString()));
        });

        assignmentDao.deleteAll(auctions);
    }

    @Override
    @Transactional
    public void declineAllSignupRequests(@NonNull ShiftPlan shiftPlan) {
        Collection<Assignment> requests = assignmentDao.findSignupRequestsByShiftPlanId(shiftPlan.getId());

        // publish events
        requests.forEach(request -> {
            notificationRequests.request(new NotificationRequest(java.util.UUID.randomUUID(),
                request.getAssignedVolunteer().getId(), NotificationTypes.ASSIGNMENT_JOIN_DECLINED,
                NotificationContext.encode(String.valueOf(shiftPlan.getEvent().getId()), request.getPositionSlot().getName(), null)));
            audit.requestDeclined(request);
            publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotJoinRequestDenied(request.getPositionSlot(), request.getAssignedVolunteer().getId().toString()));
        });

        assignmentDao.deleteAll(requests);
    }

    @Override
    public @NonNull Collection<Assignment> getAllAssignmentsForUser(@NonNull ShiftPlan plan, @NonNull Volunteer volunteer) {
        return assignmentDao.findAssignmentsForShiftPlanAndUser(plan.getId(), volunteer.getId());
    }
}
