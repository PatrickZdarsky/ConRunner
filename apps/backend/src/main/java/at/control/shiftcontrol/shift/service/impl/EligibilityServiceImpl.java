package at.control.shiftcontrol.shift.service.impl;

import java.time.Instant;
import java.util.Collection;

import at.control.shiftcontrol.identity.Authorities;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.platform.exception.ConflictException;
import at.control.shiftcontrol.platform.exception.IllegalStateException;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.type.PositionSignupState;
import at.control.shiftcontrol.shift.lib.type.TradeStatus;
import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.dao.AssignmentDao;
import at.control.shiftcontrol.shift.dao.PositionSlotDao;
import at.control.shiftcontrol.shift.dao.TimeConstraintDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotJoinErrorDto;
import at.control.shiftcontrol.shift.service.EligibilityService;

@Service
@RequiredArgsConstructor
public class EligibilityServiceImpl implements EligibilityService {
    private final VolunteerDao volunteerDao;
    private final PositionSlotDao positionSlotDao;
    private final AssignmentDao assignmentDao;
    private final TimeConstraintDao timeConstraintDao;

    private final CurrentShiftActor currentActor;

    @Override
    public @NonNull PositionSignupState getSignupStateForPositionSlot(@NonNull Long positionSlotId, @NonNull String userId) {
        return getSignupStateForPositionSlot(
            positionSlotDao.getById(positionSlotId),
            volunteerDao.getById(java.util.UUID.fromString(userId))
        );
    }

    @Override
    public @NonNull PositionSignupState getSignupStateForPositionSlot(@NonNull PositionSlot positionSlot, @NonNull String userId) {
        return getSignupStateForPositionSlot(
            positionSlot,
            volunteerDao.getById(java.util.UUID.fromString(userId))
        );
    }

    @Override
    public @NonNull PositionSignupState getSignupStateForPositionSlot(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer) {
        return getSignupStateForPositionSlotExcludingSlot(positionSlot, volunteer, null);
    }

    @Override
    public @NonNull PositionSignupState getSignupStateForPositionSlotExcludingSlot(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer, @Nullable PositionSlot slotToExclude) {
        if (isSignedUp(positionSlot, volunteer)) {
            return PositionSignupState.SIGNED_UP;
        }

        boolean eligibleForRole = positionSlot.getRole() == null || (volunteer.getRoles() != null && volunteer.getRoles().contains(positionSlot.getRole()));
        if (!eligibleForRole) {
            return PositionSignupState.NOT_ELIGIBLE;
        }

        var conflictingConstraints =
            timeConstraintDao.findByPositionSlotIdAndVolunteerId(positionSlot.getId(), volunteer.getId().toString());
        if (!conflictingConstraints.isEmpty()) {
            return PositionSignupState.TIME_CONFLICT_TIME_CONSTRAINT;
        }

        Collection<Assignment> conflictingAssignments;
        if (slotToExclude != null) {
            conflictingAssignments = assignmentDao.getConflictingAssignmentsExcludingSlot(
                volunteer.getId().toString(), slotToExclude
            );
        } else {
            conflictingAssignments = assignmentDao.getConflictingAssignments(volunteer.getId().toString(), positionSlot);
        }
        if (!conflictingAssignments.isEmpty()) {
            return PositionSignupState.TIME_CONFLICT_ASSIGNMENT;
        }

        //Positive cases
        if (hasAuction(positionSlot)) {
            return PositionSignupState.SIGNUP_VIA_AUCTION;
        }

        boolean hasCapacity = hasCapacity(positionSlot);
        boolean isTradePossible = hasOpenTradeForUser(positionSlot, volunteer.getId().toString());

        if (hasCapacity && isTradePossible) {
            return PositionSignupState.SIGNUP_OR_TRADE;
        }

        if (hasCapacity) {
            return PositionSignupState.SIGNUP_POSSIBLE;
        }

        // Slot is full: check alternative mechanisms
        if (isTradePossible) {
            return PositionSignupState.SIGNUP_VIA_TRADE;
        }

        // No special mechanisms, just full
        return PositionSignupState.FULL;
    }

    @Override
    public void validateSignUpStateForJoin(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer) {
        PositionSignupState signupState = this.getSignupStateForPositionSlot(positionSlot, volunteer);
        switch (signupState) {
            case SIGNED_UP, FULL, SIGNUP_VIA_TRADE, SIGNUP_VIA_AUCTION, TIME_CONFLICT_ASSIGNMENT, TIME_CONFLICT_TIME_CONSTRAINT:
                // simply joining is not possible
                throw new ConflictException(new PositionSlotJoinErrorDto(signupState));
            case NOT_ELIGIBLE:
                if (!currentActor.get().hasAuthority(Authorities.CAN_JOIN_UNELIGIBLE_POSITIONS)) {
                    throw new ConflictException(new PositionSlotJoinErrorDto(signupState));
                }
                break;
            case SIGNUP_POSSIBLE, SIGNUP_OR_TRADE:
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + signupState);
        }
    }

    @Override
    public void validateSignUpStateForAuction(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer) {
        PositionSignupState signupState = this.getSignupStateForPositionSlot(positionSlot, volunteer);
        switch (signupState) {
            case SIGNED_UP, FULL, SIGNUP_VIA_TRADE, SIGNUP_POSSIBLE, SIGNUP_OR_TRADE, TIME_CONFLICT_ASSIGNMENT, TIME_CONFLICT_TIME_CONSTRAINT:
                throw new ConflictException(new PositionSlotJoinErrorDto(signupState));
            case NOT_ELIGIBLE:
                if (!currentActor.get().hasAuthority(Authorities.CAN_JOIN_UNELIGIBLE_POSITIONS)) {
                    throw new ConflictException(new PositionSlotJoinErrorDto(signupState));
                }
                break;
            case SIGNUP_VIA_AUCTION:
                // only possible status for claiming an auction
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + signupState);
        }
    }

    @Override
    public boolean isEligibleAndNotSignedUp(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer) {
        PositionSignupState signupState = this.getSignupStateForPositionSlot(positionSlot, volunteer);
        return !PositionSignupState.SIGNED_UP.equals(signupState)
            && !PositionSignupState.isConflicts(signupState);
    }

    @Override
    public boolean isEligibleAndNotSignedUpExcludingSlot(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer, @NonNull PositionSlot slotToExclude) {
        PositionSignupState signupState = this.getSignupStateForPositionSlotExcludingSlot(positionSlot, volunteer, slotToExclude);
        return !PositionSignupState.SIGNED_UP.equals(signupState)
            && !PositionSignupState.isConflicts(signupState);
    }

    @Override
    public void validateIsEligibleAndNotSignedUp(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer) {
        if (!isEligibleAndNotSignedUp(positionSlot, volunteer)) {
            throw new ConflictException("position slot can not be assigned to volunteer");
        }
    }

    @Override
    public void validateIsEligibleAndNotSignedUpExcludingSlot(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer, @NonNull PositionSlot slotToExclude) {
        if (!isEligibleAndNotSignedUpExcludingSlot(positionSlot, volunteer, slotToExclude)) {
            throw new ConflictException("position slot can not be assigned to volunteer");
        }
    }

    @Override
    public @NonNull Collection<Assignment> getConflictingAssignments(@NonNull String volunteerId, @NonNull Instant startTime, @NonNull Instant endTime) {
        return assignmentDao.getConflictingAssignments(volunteerId, startTime, endTime);
    }

    @Override
    public @NonNull Collection<Assignment> getConflictingAssignments(@NonNull String volunteerId, @NonNull PositionSlot positionSlot) {
        return getConflictingAssignments(volunteerId, positionSlot.getShift().getStartTime(), positionSlot.getShift().getEndTime());
    }

    @Override
    public @NonNull Collection<Assignment> getConflictingAssignmentsExcludingShift(@NonNull String volunteerId, @NonNull Instant startTime, @NonNull Instant endTime, long shiftIdToExclude) {
        return assignmentDao.getConflictingAssignmentsExcludingShift(volunteerId, startTime, endTime, shiftIdToExclude);
    }

    @Override
    public @NonNull Collection<Assignment> getConflictingAssignmentsExcludingShift(@NonNull String volunteerId, @NonNull PositionSlot positionSlot, long shiftIdToExclude) {
        return getConflictingAssignmentsExcludingShift(
            volunteerId, positionSlot.getShift().getStartTime(), positionSlot.getShift().getEndTime(), shiftIdToExclude);
    }

    @Override
    public void validateHasConflictingAssignmentsExcludingShift(@NonNull String volunteerId, @NonNull Instant startTime, @NonNull Instant endTime, long shiftId) {
        var a = assignmentDao.getConflictingAssignmentsExcludingShift(volunteerId, startTime, endTime, shiftId);
        if (a.isEmpty()) {
            return;
        }
        throw new ConflictException("User has conflicting assignments");
    }

    @Override
    public void validateHasConflictingAssignmentsExcludingShift(@NonNull String volunteerId, @NonNull PositionSlot positionSlot, long shiftToExclude) {
        validateHasConflictingAssignmentsExcludingShift(
            volunteerId, positionSlot.getShift().getStartTime(), positionSlot.getShift().getEndTime(), shiftToExclude);
    }

    @Override
    public boolean hasCapacity(@NonNull PositionSlot positionSlot) {
        if (positionSlot.getAssignments() != null) {
            return positionSlot.getAssignments().stream()
                .filter(a -> a.getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT).toList()
                .size() < positionSlot.getDesiredVolunteerCount();
        }
        return true;
    }

    private boolean isSignedUp(PositionSlot positionSlot, Volunteer volunteer) {
        if (positionSlot.getAssignments() == null) {
            return false;
        }

        for (var assignment : positionSlot.getAssignments()) {
            if (assignment.getAssignedVolunteer() != null && assignment.getAssignedVolunteer().getId().equals(volunteer.getId())) {
                var status = assignment.getStatus();
                return status != AssignmentStatus.REQUEST_FOR_ASSIGNMENT; // pending requests do not count as signed up
            }
        }
        return false;
    }

    private boolean hasAuction(PositionSlot slot) {
        return slot.getAssignments() != null && slot.getAssignments().stream()
            .anyMatch(a -> a.getStatus() == AssignmentStatus.AUCTION || a.getStatus() == AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN);
    }

    // trade requests where the requested assignment is assigned to the user
    private boolean hasOpenTradeForUser(PositionSlot slot, String userId) {
        return slot.getAssignments() != null && slot.getAssignments().stream().anyMatch(assignment ->
            assignment.getOutgoingSwitchRequests().stream().anyMatch(req ->
                req.getStatus() == TradeStatus.OPEN
                    && req.getRequestedAssignment().getAssignedVolunteer().getId().toString().equals(userId)
            )
        );
    }
}
