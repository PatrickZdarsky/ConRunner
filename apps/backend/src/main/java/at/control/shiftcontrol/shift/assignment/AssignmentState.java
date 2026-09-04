package at.control.shiftcontrol.shift.assignment;

/** Current workflow state of a volunteer's relation to one position slot. */
public enum AssignmentState {
    ACCEPTED,
    JOIN_REQUESTED,
    LEAVE_REQUESTED,
    DECLINED,
    WITHDRAWN,
    UNASSIGNED;

    /** Final records may be reused so one slot/volunteer pair has a continuous workflow history. */
    public boolean mayTransitionTo(AssignmentState next) {
        if (next == null || next == this) {
            return false;
        }
        return switch (this) {
            case ACCEPTED -> next == LEAVE_REQUESTED || next == UNASSIGNED;
            case JOIN_REQUESTED -> next == ACCEPTED || next == DECLINED || next == WITHDRAWN;
            case LEAVE_REQUESTED -> next == ACCEPTED || next == UNASSIGNED;
            case DECLINED, WITHDRAWN, UNASSIGNED -> next == JOIN_REQUESTED || next == ACCEPTED;
        };
    }

    public static boolean mayCreateAs(AssignmentState next) {
        return next == ACCEPTED || next == JOIN_REQUESTED;
    }
}
