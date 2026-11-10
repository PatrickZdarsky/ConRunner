package at.control.shiftcontrol.communications.request;

import at.control.shiftcontrol.communications.api.NotificationTypes;

import java.util.Map;

/** Notification text is owned by communications; domain modules supply only safe context. */
public final class NotificationTemplates {
    public record Template(String title, String body) {}
    private static final Map<String, Template> TEMPLATES = Map.ofEntries(
        Map.entry(NotificationTypes.TRADE_REQUESTED, new Template("Trade requested", "A volunteer requested a trade with you.")),
        Map.entry(NotificationTypes.TRADE_ACCEPTED, new Template("Trade accepted", "Your trade request was accepted.")),
        Map.entry(NotificationTypes.TRADE_DECLINED, new Template("Trade declined", "Your trade request was declined.")),
        Map.entry(NotificationTypes.ROLE_ASSIGNED, new Template("Role assigned", "A role was assigned to you.")),
        Map.entry(NotificationTypes.ROLE_UNASSIGNED, new Template("Role removed", "A role was removed from your profile.")),
        Map.entry(NotificationTypes.ASSIGNMENT_JOIN_REQUESTED, new Template("Join requested", "A volunteer requested to join a position.")),
        Map.entry(NotificationTypes.ASSIGNMENT_LEAVE_REQUESTED, new Template("Leave requested", "A volunteer requested to leave a position.")),
        Map.entry(NotificationTypes.ASSIGNMENT_JOIN_ACCEPTED, new Template("Join request accepted", "Your request to join a position was accepted.")),
        Map.entry(NotificationTypes.ASSIGNMENT_JOIN_DECLINED, new Template("Join request declined", "Your request to join a position was declined.")),
        Map.entry(NotificationTypes.ASSIGNMENT_LEAVE_ACCEPTED, new Template("Leave request accepted", "Your request to leave a position was accepted.")),
        Map.entry(NotificationTypes.ASSIGNMENT_LEAVE_DECLINED, new Template("Leave request declined", "Your request to leave a position was declined.")),
        Map.entry(NotificationTypes.MEMBERSHIP_CHANGED, new Template("Plans changed", "Your plan memberships were changed.")),
        Map.entry(NotificationTypes.ROLES_CHANGED, new Template("Roles changed", "Your plan roles were changed.")),
        Map.entry(NotificationTypes.VOLUNTEER_LOCKED, new Template("Plan access locked", "Your access to a plan was locked.")),
        Map.entry(NotificationTypes.VOLUNTEER_UNLOCKED, new Template("Plan access unlocked", "Your access to a plan was unlocked.")),
        Map.entry(NotificationTypes.VOLUNTEER_RESET, new Template("Assignments reset", "Your assignments in a plan were reset.")),
        Map.entry(NotificationTypes.AUCTION_CLAIMED, new Template("Auction claimed", "Another volunteer claimed your auction.")),
        Map.entry(NotificationTypes.PLAN_VOLUNTEER_JOINED, new Template("Volunteer joined", "A volunteer joined your plan.")),
        Map.entry(NotificationTypes.PLAN_PLANNER_JOINED, new Template("Planner joined", "A planner joined a plan.")),
        Map.entry(NotificationTypes.TRUST_ALERT, new Template("Trust alert", "A new trust alert requires review."))
    );
    private NotificationTemplates() {}
    public static Template get(String type) {
        var template = TEMPLATES.get(type);
        if (template == null) throw new IllegalArgumentException("Unsupported notification type");
        return template;
    }
    public static boolean supports(String type) { return type != null && TEMPLATES.containsKey(type); }
    public static java.util.List<String> types() { return TEMPLATES.keySet().stream().sorted().toList(); }
}
