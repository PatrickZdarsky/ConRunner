package at.conrunner.shift.lib.type;

import java.util.EnumSet;

public enum AssignmentStatus {
    /**
     * The assignment has been accepted by the user, or he has assigned themselves to it.
     */
    ACCEPTED,
    /**
     * The assignment is in the auction state, waiting for others to take it.
     */
    AUCTION,
    /**
     * The assignment is in the auction state, but the assigned user has requested the shiftplanner to unassign themselves.
     */
    AUCTION_REQUEST_FOR_UNASSIGN,
    /**
     * The volunteer wants to join an assignment when planning phase is over.
     */
    REQUEST_FOR_ASSIGNMENT;

    public static final EnumSet<AssignmentStatus> ACTIVE_AUCTION_STATES =
        EnumSet.of(AUCTION, AUCTION_REQUEST_FOR_UNASSIGN);

    public static final EnumSet<AssignmentStatus> REQUEST_STATES =
        EnumSet.of(REQUEST_FOR_ASSIGNMENT, AUCTION_REQUEST_FOR_UNASSIGN);
}
