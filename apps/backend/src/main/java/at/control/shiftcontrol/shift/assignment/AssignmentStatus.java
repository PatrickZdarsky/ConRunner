package at.control.shiftcontrol.shift.assignment;

/** Legacy live-assignment states. Removal is represented by deleting the live Assignment. */
public enum AssignmentStatus {
    ACCEPTED,
    AUCTION,
    AUCTION_REQUEST_FOR_UNASSIGN,
    REQUEST_FOR_ASSIGNMENT
}
