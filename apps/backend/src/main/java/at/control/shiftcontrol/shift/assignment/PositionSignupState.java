package at.control.shiftcontrol.shift.assignment;

/** Stable legacy-oriented summary for UI clients; detailed reasons remain available separately. */
public enum PositionSignupState {
    SIGNED_UP,
    NOT_ELIGIBLE,
    TIME_CONFLICT_TIME_CONSTRAINT,
    TIME_CONFLICT_ASSIGNMENT,
    SIGNUP_VIA_AUCTION,
    SIGNUP_OR_TRADE,
    SIGNUP_POSSIBLE,
    SIGNUP_VIA_TRADE,
    FULL
}
