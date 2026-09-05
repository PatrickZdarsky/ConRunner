package at.control.shiftcontrol.shift.assignment;

import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.shift.plan.StaffingMode;

/** Machine-readable detail; {@link #signupState()} preserves the established UI-level contract. */
public record AssignmentEligibility(UUID positionSlotId, UUID volunteerId, StaffingMode effectiveMode, int filledCount,
                                    int desiredVolunteerCount, Set<AssignmentEligibilityReason> reasons) {
    public AssignmentEligibility {
        reasons = Set.copyOf(reasons);
    }

    public boolean eligible() {
        return reasons.isEmpty();
    }

    public PositionSignupState signupState() {
        if (reasons.contains(AssignmentEligibilityReason.ALREADY_ASSIGNED_TO_SLOT)) {
            return PositionSignupState.SIGNED_UP;
        }
        if (reasons.contains(AssignmentEligibilityReason.UNAVAILABLE)) {
            return PositionSignupState.TIME_CONFLICT_TIME_CONSTRAINT;
        }
        if (reasons.contains(AssignmentEligibilityReason.OVERLAPPING_ASSIGNMENT)) {
            return PositionSignupState.TIME_CONFLICT_ASSIGNMENT;
        }
        if (reasons.contains(AssignmentEligibilityReason.SLOT_FULL)) {
            return PositionSignupState.FULL;
        }
        return eligible() ? PositionSignupState.SIGNUP_POSSIBLE : PositionSignupState.NOT_ELIGIBLE;
    }
}
