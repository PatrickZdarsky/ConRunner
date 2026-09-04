package at.control.shiftcontrol.shift.assignment;

/** Result of a successful direct assignment command, including any planner override used. */
public record AssignmentDecision(AssignmentDetails assignment, AssignmentEligibility eligibility, boolean overrideApplied) {
}
