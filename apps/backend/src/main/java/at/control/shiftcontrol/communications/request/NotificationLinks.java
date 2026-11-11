package at.control.shiftcontrol.communications.request;

import at.control.shiftcontrol.communications.api.NotificationTypes;

/** Maps trusted notification types and validated identifiers to relative application links. */
final class NotificationLinks {
    private static final String ID = "[1-9][0-9]{0,18}";

    private NotificationLinks() {}

    static String forRequest(String type, String data) {
        if (NotificationTypes.TRUST_ALERT.equals(type) && "global".equals(data)) return "/trust";
        if (NotificationTypes.PLAN_PLANNER_JOINED.equals(type) && data != null && data.startsWith("volunteer:")) {
            try {
                var id = java.util.UUID.fromString(data.substring("volunteer:".length()));
                if (!("volunteer:" + id).equals(data)) throw new IllegalArgumentException("Invalid volunteer identifier");
                return "/volunteers/" + id;
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Invalid volunteer identifier", invalid);
            }
        }

        // Older queued requests contain just the event ID; keep them deliverable.
        if (data != null && data.matches(ID)) return "/events/" + data;

        if (data == null || !data.matches(ID + ":" + ID))
            throw new IllegalArgumentException("Invalid notification context");
        var ids = data.split(":", 2);
        var base = "/events/" + ids[0] + "/plans?planId=" + ids[1];
        return switch (type) {
            case NotificationTypes.ASSIGNMENT_JOIN_REQUESTED -> base + "&mode=assignments&status=REQUEST_FOR_ASSIGNMENT";
            case NotificationTypes.ASSIGNMENT_LEAVE_REQUESTED -> base + "&mode=assignments&status=AUCTION_REQUEST_FOR_UNASSIGN";
            case NotificationTypes.PLAN_VOLUNTEER_JOINED -> base + "&mode=users";
            default -> throw new IllegalArgumentException("Unexpected plan context for notification type");
        };
    }
}
