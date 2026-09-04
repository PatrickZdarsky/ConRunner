package at.control.shiftcontrol.shift.assignment;

/** Stable values for clients to render eligibility guidance without parsing messages. */
public enum AssignmentEligibilityReason {
    NOT_PLAN_MEMBER,
    VOLUNTEER_CHANGES_RESTRICTED,
    REQUIRED_ROLE_MISSING,
    EFFECTIVE_MODE_DOES_NOT_ALLOW_DIRECT_SIGNUP,
    ALREADY_ASSIGNED_TO_SLOT,
    JOIN_REQUEST_ALREADY_EXISTS,
    LEAVE_REQUEST_ALREADY_EXISTS,
    SLOT_FULL,
    OVERLAPPING_ASSIGNMENT,
    UNAVAILABLE
}
