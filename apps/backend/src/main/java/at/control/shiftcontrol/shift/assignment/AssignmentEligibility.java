package at.control.shiftcontrol.shift.assignment;

import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.shift.plan.StaffingMode;

/** Machine-readable decision inputs, including capacity observed while the relevant slot is locked. */
public record AssignmentEligibility(UUID positionSlotId, UUID volunteerId, StaffingMode effectiveMode, int filledCount,
                                    int desiredVolunteerCount, Set<AssignmentEligibilityReason> reasons) {
    public AssignmentEligibility {
        reasons = Set.copyOf(reasons);
    }

    public boolean eligible() {
        return reasons.isEmpty();
    }
}
