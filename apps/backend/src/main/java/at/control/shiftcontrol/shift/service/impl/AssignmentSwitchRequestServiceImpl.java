package at.control.shiftcontrol.shift.service.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentKey;
import at.control.shiftcontrol.shift.entity.AssignmentPair;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequestKey;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.event.events.TradeEvent;
import at.control.shiftcontrol.platform.exception.ForbiddenException;
import at.control.shiftcontrol.platform.exception.IllegalStateException;
import at.control.shiftcontrol.platform.exception.StateViolationException;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.type.PositionSignupState;
import at.control.shiftcontrol.shift.lib.type.TradeStatus;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dao.AssignmentDao;
import at.control.shiftcontrol.shift.dao.AssignmentSwitchRequestDao;
import at.control.shiftcontrol.shift.dao.PositionSlotDao;
import at.control.shiftcontrol.shift.dao.ShiftDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.trade.TradeCandidatesDto;
import at.control.shiftcontrol.shift.dto.trade.TradeCreateDto;
import at.control.shiftcontrol.shift.dto.trade.TradeDto;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;
import at.control.shiftcontrol.shift.mapper.PositionSlotAssemblingMapper;
import at.control.shiftcontrol.shift.mapper.TradeMapper;
import at.control.shiftcontrol.shift.service.AssignmentService;
import at.control.shiftcontrol.shift.service.AssignmentSwitchRequestService;
import at.control.shiftcontrol.shift.service.EligibilityService;
import at.control.shiftcontrol.shift.util.LockStatusHelper;
import at.control.shiftcontrol.shift.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class AssignmentSwitchRequestServiceImpl implements AssignmentSwitchRequestService {
    @Lazy
    @Autowired(required = false)
    private AssignmentSwitchRequestService self;
    private final AssignmentSwitchRequestDao assignmentSwitchRequestDao;
    private final AssignmentDao assignmentDao;
    private final PositionSlotDao positionSlotDao;
    private final VolunteerDao volunteerDao;

    private final EligibilityService eligibilityService;
    private final AssignmentService assignmentService;
    private final PositionSlotAssemblingMapper positionSlotAssemblingMapper;
    private final SecurityHelper securityHelper;
    private final ApplicationEventPublisher publisher;
    private final TradeMapper tradeMapper;
    private final ShiftDao shiftDao;

    @Override
    public @NonNull TradeDto getTradeById(long id) {
        AssignmentSwitchRequest trade = assignmentSwitchRequestDao.getById(id);
        return tradeMapper.toDto(trade);
    }

    @Override
    public @NonNull Collection<TradeCandidatesDto> getPositionSlotsToOffer(long requestedPositionSlotId, @NonNull String currentUserId) {
        // get requested PositionSlot
        PositionSlot requestedPositionSlot = positionSlotDao.getById(requestedPositionSlotId);

        securityHelper.assertUserIsVolunteer(requestedPositionSlot, false);

        // get volunteers assigned to PositionSlot
        Collection<Volunteer> assignedVolunteers = assignmentDao
            .getActiveAssignmentsOfSlot(requestedPositionSlotId).stream().map(Assignment::getAssignedVolunteer).toList();
        // return if no volunteers exist to trade with
        if (assignedVolunteers.isEmpty()) {
            return List.of();
        }
        // check if user is already assigned to requested position slot
        if (assignedVolunteers.stream().anyMatch(v -> currentUserId.equals(v.getId()))) {
            throw new StateViolationException("You are already assigned to this slot");
        }

        // only check for locked, eligibility and conflict of current user when creating actual trade

        // get all assignments in this shift-plan the current user can offer
        Collection<Assignment> assignments = assignmentDao.findActiveAssignmentsForShiftPlanAndUser(
            requestedPositionSlot.getShift().getShiftPlan().getId(), currentUserId);

        Collection<Assignment> timeConflictingAssignments = new LinkedList<>();
        var signupStateRequested = eligibilityService.getSignupStateForPositionSlot(requestedPositionSlot, currentUserId);
        if (signupStateRequested == PositionSignupState.TIME_CONFLICT_ASSIGNMENT) {
            // only offer trades if the requested slot has a time conflict with an existing assignment
            var conflictingAssignments =
                assignmentDao.getConflictingAssignmentsExcludingSlot(currentUserId, requestedPositionSlot);
            timeConflictingAssignments = assignments.stream()
                .filter(a -> conflictingAssignments.stream().anyMatch(ca -> ca.getId() == a.getId()))
                .toList();
        }

        // hanlde case with time conflicts separately
        Collection<TradeCandidatesDto> slotsToOffer = new LinkedList<>();
        if (!timeConflictingAssignments.isEmpty()) {
            // only offer assignments that have a time conflict with the requested slot
            for (Assignment a : timeConflictingAssignments) {
                // only offer the position to eligible volunteers with no conflicts (besides the time conflict with the requested slot)
                Collection<Volunteer> usersToTradeWith = assignedVolunteers.stream()
                    .filter(v -> {
                        var signupStateOffered = eligibilityService.getSignupStateForPositionSlot(a.getPositionSlot(), v.getId());
                        return signupStateOffered != PositionSignupState.SIGNED_UP && signupStateOffered != PositionSignupState.NOT_ELIGIBLE
                            && signupStateOffered != PositionSignupState.TIME_CONFLICT_TIME_CONSTRAINT;
                    })
                    .toList();
                if (!usersToTradeWith.isEmpty()) {
                    slotsToOffer.add(
                        positionSlotAssemblingMapper.tradeCandidatesDto(a.getPositionSlot(), usersToTradeWith));
                }
            }
        } else {
            // normal case
            // for each assignment, check to which volunteers (of the requested slot) it can be offered
            for (Assignment a : assignments) {
                // only offer the position to eligible volunteers with no conflicts
                Collection<Volunteer> usersToTradeWith = getPossibleUsersForTrade(a.getPositionSlot(), requestedPositionSlot, assignedVolunteers);
                if (!usersToTradeWith.isEmpty()) {
                    slotsToOffer.add(
                        positionSlotAssemblingMapper.tradeCandidatesDto(a.getPositionSlot(), usersToTradeWith));
                }
            }
        }

        // remove candidates where the current user already requested a trade
        return removeExistingTrades(slotsToOffer, requestedPositionSlotId, currentUserId);
    }

    private Collection<Volunteer> getPossibleUsersForTrade(PositionSlot offeredPositionSlot, PositionSlot requestedPositionSlot,
                                                           Collection<Volunteer> volunteers) {
        return volunteers.stream().filter(v -> isTradePossibleForUser(offeredPositionSlot, requestedPositionSlot, v)).toList();
    }

    private boolean isTradePossibleForUser(PositionSlot offeredPositionSlot, PositionSlot requestedPositionSlot, Volunteer volunteer) {
        // check if assignable and has no conflicts
        return eligibilityService.isEligibleAndNotSignedUpExcludingSlot(offeredPositionSlot, volunteer, requestedPositionSlot);
    }

    private Collection<TradeCandidatesDto> removeExistingTrades(Collection<TradeCandidatesDto> slotsToOffer, long requestedPositionSlotId,
                                                                String currentUserId) {
        // check for already existing trades in status OPEN
        Collection<AssignmentSwitchRequest> existingTrades =
            assignmentSwitchRequestDao.findOpenTradesForRequestedPositionAndOfferingUser(requestedPositionSlotId, currentUserId);

        // convert to set of keys for easier lookup
        Set<AssignmentSwitchRequestKey> existingTradeKeys =
            existingTrades.stream()
                .map(tr -> AssignmentSwitchRequestKey.of(
                    tr.getOfferingAssignment(),
                    tr.getRequestedAssignment()
                ))
                .collect(Collectors.toSet());

        // remove all volunteers for each possible slot to offer, where a trade already exists
        //  this does not check for existing inverse trade requests
        return slotsToOffer.stream()
            .map(candidate -> {
                List<VolunteerDto> filteredVolunteers =
                    candidate.getEligibleTradeRecipients().stream()
                        .filter(volunteer -> {
                            AssignmentSwitchRequestKey key = AssignmentSwitchRequestKey.of(
                                AssignmentKey.of(
                                    ConvertUtil.idToLong(candidate.getOwnPosition().getId()),
                                    currentUserId),
                                AssignmentKey.of(
                                    requestedPositionSlotId,
                                    volunteer.getId())
                            );
                            // removes volunteers from the candidate list, where a trade already exists
                            return !existingTradeKeys.contains(key);
                        })
                        .toList();
                var shift = shiftDao.getById(ConvertUtil.idToLong(candidate.getOwnPosition().getAssociatedShiftId()));
                return new TradeCandidatesDto(
                    candidate.getOwnPosition(),
                    shift.getName(),
                    shift.getStartTime(),
                    filteredVolunteers
                );
            })
            // remove dto if no volunteers are left
            .filter(c -> !c.getEligibleTradeRecipients().isEmpty())
            .toList();
    }

    @Override
    @Transactional
    public @NonNull Collection<TradeDto> createTrade(@NonNull TradeCreateDto tradeCreateDto, @NonNull String currentUserId) {
        // get current user (volunteer)
        Volunteer currentUser = volunteerDao.getById(currentUserId);

        // get requested PositionSlot
        PositionSlot requestedPositionSlot = positionSlotDao.getById(ConvertUtil.idToLong(tradeCreateDto.getRequestedPositionSlotId()));
        securityHelper.assertVolunteerIsVolunteer(requestedPositionSlot, currentUser, true);

        // get offering assignment
        PositionSlot offeredPositionSlot = positionSlotDao.getById(ConvertUtil.idToLong(tradeCreateDto.getOfferedPositionSlotId()));
        securityHelper.assertVolunteerIsVolunteer(offeredPositionSlot, currentUser, true);
        // check if shifts are locked
        if (LockStatusHelper.isLocked(requestedPositionSlot) || LockStatusHelper.isLocked(offeredPositionSlot)) {
            throw new StateViolationException("Trade not possible, shift plan is locked");
        }

        Assignment offeredAssignment = offeredPositionSlot.getAssignments().stream()
            .filter(assignment -> assignment.getAssignedVolunteer().getId().equals(currentUser.getId())
                && assignment.getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT).findFirst()
            .orElseThrow(() -> new ForbiddenException("You are not assigned to the offered position"));

        // check if eligible and for conflicts
        validateTradePossible(offeredPositionSlot, requestedPositionSlot, currentUser);

        // get Volunteer entity for users to create trades with
        Collection<Volunteer> requestedVolunteers = getVolunteersToTradeWith(requestedPositionSlot, tradeCreateDto.getRequestedVolunteerIds());
        for (Volunteer v : requestedVolunteers) {
            // check if eligible and for conflicts
            validateTradePossible(requestedPositionSlot, offeredPositionSlot, v);
        }

        // create switch requests for each requested user
        Collection<AssignmentSwitchRequest> trades = requestedPositionSlot.getAssignments().stream()
            .filter(assignment ->
                tradeCreateDto.getRequestedVolunteerIds().stream().anyMatch(
                    volunteerId -> Objects.equals(volunteerId, String.valueOf(assignment.getAssignedVolunteer().getId()))))
            .map(requestedAssignment -> createAssignmentSwitchRequest(offeredAssignment, requestedAssignment)).toList();

        // check for existing trades
        Collection<AssignmentPair> pairs = trades.stream().map(
            t -> AssignmentPair.of(t.getOfferingAssignment().getId(), t.getRequestedAssignment().getId())).toList();

        Collection<AssignmentSwitchRequest> existingAnyStatus =
            assignmentSwitchRequestDao.findAllByAssignmentPairs(pairs);

        var existingByPair = existingAnyStatus.stream()
            .collect(Collectors.toMap(
                t -> AssignmentPair.of(t.getOfferingAssignment().getId(), t.getRequestedAssignment().getId()),
                t -> t,
                (a, b) -> a
            ));

        // either insert new trade or update existing trade to OPEN
        trades = trades.stream()
            .map(t -> {
                AssignmentPair pair = AssignmentPair.of(t.getOfferingAssignment().getId(), t.getRequestedAssignment().getId());
                AssignmentSwitchRequest existing = existingByPair.get(pair);
                if (existing == null) {
                    return t; // create new
                }
                if (existing.getStatus() == TradeStatus.OPEN) {
                    return null; // already open, skip
                }
                existing.setStatus(TradeStatus.OPEN);
                existing.setCreatedAt(Instant.now());
                return existing; // re-open existing
            })
            .filter(Objects::nonNull)
            .toList();

        // check if trade in other direction already exists
        //      accept if exists
        for (AssignmentSwitchRequest trade : trades) {
            List<AssignmentSwitchRequest> inverse = assignmentSwitchRequestDao.findInverseTrade(trade)
                .stream()
                .filter(x -> x.getStatus().equals(TradeStatus.OPEN))
                .toList();
            if (inverse.size() > 1) {
                throw new IllegalStateException("more than one inverse trade is open" + inverse);
            }
            if (!inverse.isEmpty()) {
                return List.of(self.acceptTrade(inverse.get(0).getId(), currentUserId));
            }
        }

        trades = assignmentSwitchRequestDao.saveAll(trades);

        trades.forEach(trade -> publisher.publishEvent(TradeEvent.tradeCanceled(trade)));
        return tradeMapper.toDto(trades);
    }

    private Collection<Volunteer> getVolunteersToTradeWith(PositionSlot positionSlot, Collection<String> volunteerIds) {
        var existingVolunteerIds = volunteerDao.findAllByVolunteerIds(volunteerIds)
            .stream()
            .map(Volunteer::getId)
            .collect(Collectors.toSet());
        return positionSlot.getAssignments().stream()
            .filter(assignment -> assignment.getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT)
            .map(Assignment::getAssignedVolunteer)
            .filter(v -> existingVolunteerIds.contains(v.getId()))
            .toList();
    }

    @Override
    @Transactional
    public @NonNull TradeDto acceptTrade(long id, @NonNull String currentUserId) {
        // get trade
        AssignmentSwitchRequest trade = assignmentSwitchRequestDao.getById(id);

        securityHelper.assertVolunteerIsNotLockedInPlan(trade.getOfferingAssignment());
        securityHelper.assertVolunteerIsNotLockedInPlan(trade.getRequestedAssignment());

        // check if volunteer is owner of requested slot
        if (!trade.getRequestedAssignment().getAssignedVolunteer().getId().equals(currentUserId)) {
            throw new ForbiddenException("Trade cannot be accepted, because you are not part of it");
        }

        // check if shift is locked
        if (LockStatusHelper.isLocked(trade)) {
            throw new StateViolationException("Trade not possible, shift is locked");
        }

        // check if trade is open
        if (!trade.getStatus().equals(TradeStatus.OPEN)) {
            throw new StateViolationException("Trade is not open");
        }

        // check if none of assignment is in request signup status
        if (trade.getRequestedAssignment().getStatus() == AssignmentStatus.REQUEST_FOR_ASSIGNMENT
            || trade.getOfferingAssignment().getStatus() == AssignmentStatus.REQUEST_FOR_ASSIGNMENT) {
            throw new StateViolationException("Trade not possible, one participant is not assigned anymore");
        }

        // check for access, eligibility and conflicts for both users
        validateTradePossible(trade.getRequestedAssignment().getPositionSlot(), trade.getOfferingAssignment().getPositionSlot(),
            trade.getRequestedAssignment().getAssignedVolunteer()); // current user
        validateTradePossible(trade.getOfferingAssignment().getPositionSlot(), trade.getRequestedAssignment().getPositionSlot(),
            trade.getOfferingAssignment().getAssignedVolunteer()); // other user

        // update assignments
        AssignmentSwitchRequest executedTrade = assignmentService.executeTrade(trade);

        return tradeMapper.toDto(executedTrade);
    }

    @Override
    public @NonNull TradeDto declineTrade(long id, @NonNull String currentUserId) {
        // get trade
        AssignmentSwitchRequest trade = assignmentSwitchRequestDao.getById(id);
        securityHelper.assertVolunteerIsNotLockedInPlan(trade.getOfferingAssignment());
        securityHelper.assertVolunteerIsNotLockedInPlan(trade.getRequestedAssignment());

        // check if user can decline
        if (!trade.getRequestedAssignment().getAssignedVolunteer().getId().equals(currentUserId)) {
            throw new ForbiddenException("Trade cannot be declined, because you are not part of it");
        }
        if (!trade.getStatus().equals(TradeStatus.OPEN)) {
            throw new StateViolationException("Trade is not open");
        }

        trade.setStatus(TradeStatus.REJECTED);

        trade = assignmentSwitchRequestDao.save(trade);
        publisher.publishEvent(TradeEvent.tradeDeclined(trade));
        return tradeMapper.toDto(trade);
    }

    @Override
    public @NonNull TradeDto cancelTrade(long id, @NonNull String currentUserId) {
        // get trade
        AssignmentSwitchRequest trade = assignmentSwitchRequestDao.getById(id);
        securityHelper.assertVolunteerIsNotLockedInPlan(trade.getOfferingAssignment());
        securityHelper.assertVolunteerIsNotLockedInPlan(trade.getRequestedAssignment());
        // check if user can cancel
        if (!trade.getOfferingAssignment().getAssignedVolunteer().getId().equals(currentUserId)) {
            throw new ForbiddenException("Trade cannot be canceled, because you are not part of it");
        }
        if (!trade.getStatus().equals(TradeStatus.OPEN)) {
            throw new StateViolationException("Trade is not open");
        }

        trade.setStatus(TradeStatus.CANCELED);

        trade = assignmentSwitchRequestDao.save(trade);
        publisher.publishEvent(TradeEvent.tradeCanceled(trade));
        return tradeMapper.toDto(trade);
    }

    private AssignmentSwitchRequest createAssignmentSwitchRequest(Assignment offering, Assignment requested) {
        return AssignmentSwitchRequest.builder()
            .offeringAssignment(offering)
            .requestedAssignment(requested)
            .status(TradeStatus.OPEN)
            .createdAt(Instant.now())
            .build();
    }

    private void validateTradePossible(PositionSlot ownedSlot, PositionSlot slotToBeTaken, Volunteer volunteer) {
        // check if position slots belong to same shift plan
        if (ownedSlot.getShift().getShiftPlan().getEvent().getId() != slotToBeTaken.getShift().getShiftPlan().getEvent().getId()) {
            throw new StateViolationException("Trade cannot be created across different events");
        }

        // check if volunteer has access to shift plan
        securityHelper.assertVolunteerIsVolunteer(slotToBeTaken, volunteer, true);

        // check if user is eligible for the requested position slot and has no conflicts
        eligibilityService.validateIsEligibleAndNotSignedUpExcludingSlot(slotToBeTaken, volunteer, ownedSlot);
    }
}
