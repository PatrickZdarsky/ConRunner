package at.conrunner.communications.api;

/** Stable notification type identifiers shared by producers and the preference API. */
public final class NotificationTypes {
    private NotificationTypes() {}
    public static final String TRADE_REQUESTED = "shift.trade.requested";
    public static final String TRADE_ACCEPTED = "shift.trade.accepted";
    public static final String TRADE_DECLINED = "shift.trade.declined";
    public static final String ROLE_ASSIGNED = "shift.role.assigned";
    public static final String ROLE_UNASSIGNED = "shift.role.unassigned";
    public static final String ASSIGNMENT_JOIN_REQUESTED = "shift.assignment.join.requested";
    public static final String ASSIGNMENT_LEAVE_REQUESTED = "shift.assignment.leave.requested";
    public static final String ASSIGNMENT_JOIN_ACCEPTED = "shift.assignment.join.accepted";
    public static final String ASSIGNMENT_JOIN_DECLINED = "shift.assignment.join.declined";
    public static final String ASSIGNMENT_LEAVE_ACCEPTED = "shift.assignment.leave.accepted";
    public static final String ASSIGNMENT_LEAVE_DECLINED = "shift.assignment.leave.declined";
    public static final String MEMBERSHIP_CHANGED = "shift.membership.changed";
    public static final String ROLES_CHANGED = "shift.roles.changed";
    public static final String VOLUNTEER_LOCKED = "shift.volunteer.locked";
    public static final String VOLUNTEER_UNLOCKED = "shift.volunteer.unlocked";
    public static final String VOLUNTEER_RESET = "shift.volunteer.reset";
    public static final String AUCTION_CLAIMED = "shift.auction.claimed";
    public static final String PLAN_VOLUNTEER_JOINED = "shift.plan.volunteer_joined";
    public static final String PLAN_PLANNER_JOINED = "shift.plan.planner_joined";
    public static final String TRUST_ALERT = "shift.trust.alert";
}
