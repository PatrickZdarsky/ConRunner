package at.control.shiftcontrol.shift.assignment;

import at.control.shiftcontrol.shift.error.ShiftConflictException;
import lombok.Getter;

/** Conflict response with a structured eligibility body for an API adapter to expose. */
@Getter
public class AssignmentNotEligibleException extends ShiftConflictException {
    private final AssignmentEligibility eligibility;

    public AssignmentNotEligibleException(AssignmentEligibility eligibility) {
        super("Volunteer is not eligible for this assignment: " + eligibility.reasons());
        this.eligibility = eligibility;
    }
}
