package at.control.shiftcontrol.shift.lib.event;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NoArgsConstructor;
import lombok.NonNull;

import at.shiftcontrol.lib.exception.IllegalArgumentException;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RoutingKeys {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    public static final String ACTIVITY_CREATED = "activity.created";
    public static final String ACTIVITY_UPDATED = "activity.updated.{activityId}";
    public static final String ACTIVITY_DELETED = "activity.deleted.{activityId}";

    public static final String EVENT_CREATED = "event.created";
    public static final String EVENT_CLONED = "event.cloned.{sourceEventId}.{newEventId}";
    public static final String EVENT_UPDATED = "event.updated.{eventId}";
    public static final String EVENT_DELETED = "event.deleted.{eventId}";
    public static final String EVENT_EXPORTED = "event.exported.{eventId}.{exportFormat}";
    public static final String EVENT_IMPORTED = "event.imported.{eventId}";

    public static final String LOCATION_CREATED = "location.created";
    public static final String LOCATION_UPDATED = "location.updated.{locationId}";
    public static final String LOCATION_DELETED = "location.deleted.{locationId}";

    public static final String POSITIONSLOT_CREATED = "positionslot.created";
    public static final String POSITIONSLOT_UPDATED = "positionslot.updated.{positionSlotId}";
    public static final String POSITIONSLOT_DELETED = "positionslot.deleted.{positionSlotId}";

    public static final String POSITIONSLOT_JOINED_PREFIX = "positionslot.joined.";
    public static final String POSITIONSLOT_JOINED = POSITIONSLOT_JOINED_PREFIX + "{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_LEFT_PREFIX = "positionslot.left.";
    public static final String POSITIONSLOT_LEFT = POSITIONSLOT_LEFT_PREFIX + "{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_REQUEST_JOIN_PREFIX = "positionslot.request.join.";
    public static final String POSITIONSLOT_REQUEST_JOIN_CREATED_PREFIX = POSITIONSLOT_REQUEST_JOIN_PREFIX + "created.";
    public static final String POSITIONSLOT_REQUEST_JOIN_CREATED =  POSITIONSLOT_REQUEST_JOIN_CREATED_PREFIX + "{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_REQUEST_JOIN_WITHDRAW_PREFIX = POSITIONSLOT_REQUEST_JOIN_PREFIX + "withdraw.";
    public static final String POSITIONSLOT_REQUEST_JOIN_WITHDRAW = POSITIONSLOT_REQUEST_JOIN_WITHDRAW_PREFIX + "{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_REQUEST_JOIN_ACCEPTED = POSITIONSLOT_REQUEST_JOIN_PREFIX + "accepted.{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_REQUEST_JOIN_DECLINED = POSITIONSLOT_REQUEST_JOIN_PREFIX + "declined.{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_REQUEST_LEAVE_PREFIX = "positionslot.request.leave.";
    public static final String POSITIONSLOT_REQUEST_LEAVE_CREATED_PREFIX = POSITIONSLOT_REQUEST_LEAVE_PREFIX + "created.";
    public static final String POSITIONSLOT_REQUEST_LEAVE_CREATED = POSITIONSLOT_REQUEST_LEAVE_CREATED_PREFIX + "{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_REQUEST_LEAVE_WITHDRAW_PREFIX = POSITIONSLOT_REQUEST_LEAVE_PREFIX + "withdraw.";
    public static final String POSITIONSLOT_REQUEST_LEAVE_WITHDRAW = POSITIONSLOT_REQUEST_LEAVE_WITHDRAW_PREFIX + "{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_REQUEST_LEAVE_ACCEPTED_PREFIX = POSITIONSLOT_REQUEST_LEAVE_PREFIX + "accepted.";
    public static final String POSITIONSLOT_REQUEST_LEAVE_ACCEPTED = POSITIONSLOT_REQUEST_LEAVE_ACCEPTED_PREFIX + "{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_REQUEST_LEAVE_DECLINED_PREFIX = POSITIONSLOT_REQUEST_LEAVE_PREFIX + "declined.";
    public static final String POSITIONSLOT_REQUEST_LEAVE_DECLINED = POSITIONSLOT_REQUEST_LEAVE_DECLINED_PREFIX + "{volunteerId}.{positionSlotId}";
    public static final String POSITIONSLOT_PREFERENCE_UPDATED = "positionslot.preference.updated.{volunteerId}.{positionSlotId}";

    public static final String AUCTION_CREATED_PREFIX = "auction.created.";
    public static final String AUCTION_CREATED = AUCTION_CREATED_PREFIX + "{positionSlotId}";
    public static final String AUCTION_CLAIMED_PREFIX = "auction.claimed.";
    public static final String AUCTION_CLAIMED = AUCTION_CLAIMED_PREFIX + "{positionSlotId}.{oldVolunteerId}";
    public static final String AUCTION_CANCELED_PREFIX = "auction.canceled.";
    public static final String AUCTION_CANCELED = AUCTION_CANCELED_PREFIX + "{positionSlotId}";

    public static final String TRADE_REQUEST_CREATED_PREFIX = "trade.request.created.";
    public static final String TRADE_REQUEST_CREATED = TRADE_REQUEST_CREATED_PREFIX + "{requestedVolunteerId}.{offeringVolunteerId}";
    public static final String TRADE_REQUEST_DECLINED_PREFIX = "trade.request.declined.";
    public static final String TRADE_REQUEST_DECLINED = TRADE_REQUEST_DECLINED_PREFIX + "{requestedVolunteerId}.{offeringVolunteerId}";
    public static final String TRADE_REQUEST_CANCELED_PREFIX = "trade.request.canceled.";
    public static final String TRADE_REQUEST_CANCELED = TRADE_REQUEST_CANCELED_PREFIX + "{requestedVolunteerId}.{offeringVolunteerId}";
    public static final String TRADE_REQUEST_COMPLETED_PREFIX = "trade.request.completed.";
    public static final String TRADE_REQUEST_COMPLETED = TRADE_REQUEST_COMPLETED_PREFIX + "{requestedVolunteerId}.{offeringVolunteerId}";


    public static final String SHIFTPLAN_CREATED = "shiftplan.created";
    public static final String SHIFTPLAN_UPDATED = "shiftplan.updated.{shiftPlanId}";
    public static final String SHIFTPLAN_DELETED = "shiftplan.deleted.{shiftPlanId}";
    public static final String SHIFTPLAN_LEAVE = "shiftplan.leave.{shiftPlanId}";
    public static final String SHIFTPLAN_INVITE_CREATED = "shiftplan.invite.created.{shiftPlanId}.{inviteId}";
    public static final String SHIFTPLAN_INVITE_REVOKED = "shiftplan.invite.revoked.{shiftPlanId}.{inviteId}";
    public static final String SHIFTPLAN_INVITE_DELETED = "shiftplan.invite.deleted.{shiftPlanId}.{inviteId}";
    public static final String SHIFTPLAN_JOINED_VOLUNTEER = "shiftplan.joined.volunteer.{shiftPlanId}.{volunteerId}";
    public static final String SHIFTPLAN_JOINED_PLANNER = "shiftplan.joined.planner.{shiftPlanId}.{volunteerId}";
    public static final String SHIFTPLAN_LOCKSTATUS_CHANGED = "shiftplan.lockstatus.changed.{shiftPlanId}";

    public static final String SHIFT_CREATED = "shift.created";
    public static final String SHIFT_UPDATED = "shift.updated.{shiftId}";
    public static final String SHIFT_DELETED = "shift.deleted.{shiftId}";

    public static final String TIMECONSTRAINT_CREATED = "timeconstraint.created.{volunteerId}";
    public static final String TIMECONSTRAINT_DELETED = "timeconstraint.deleted.{volunteerId}.{timeConstraintId}";

    public static final String ROLE_CREATED = "role.created";
    public static final String ROLE_UPDATED = "role.updated.{roleId}";
    public static final String ROLE_DELETED = "role.deleted.{roleId}";
    public static final String ROLE_ASSIGNED = "role.assigned.{roleId}.{volunteerId}";
    public static final String ROLE_UNASSIGNED = "role.unassigned.{roleId}.{volunteerId}";

    public static final String VOLUNTEER_NOTIFICATION_PREFERENCE_UPDATED = "volunteer.notification.preference.updated.{volunteerId}";
    public static final String REWARDPOINTS_TRANSACTION_CREATED = "rewardpoints.transaction.created.{volunteerId}.{transactionId}";
    public static final String REWARDPOINTS_TRANSACTION_FAILED = "rewardpoints.transaction.failed.{volunteerId}";
    public static final String REWARDPOINTS_SHARETOKEN_CREATED = "rewardpoints.sharetoken.created.{shareTokenId}";
    public static final String REWARDPOINTS_SHARETOKEN_DELETED = "rewardpoints.sharetoken.deleted.{shareTokenId}";

    public static final String USERS_EVENT_BULK_ADD = "users.bulk.add";
    public static final String USERS_EVENT_BULK_REMOVE = "users.bulk.remove";

    public static final String USERS_EVENT_UPDATE = "users.{userId}.update";
    public static final String USERS_EVENT_LOCK = "users.{userId}.lock";
    public static final String USERS_EVENT_UNLOCK = "users.{userId}.unlock";
    public static final String USERS_RESET = "users.{userId}.reset";

    public static final String USERS_PLAN_BULK_ADD = "shift-plans.{shiftPlanId}.users.bulk.add";
    public static final String USERS_PLAN_BULK_REMOVE = "shift-plans.{shiftPlanId}.users.bulk.remove";

    public static final String USERS_PLAN_UPDATE = "shift-plans.{shiftPlanId}.users.{userId}";

    public static final String TRUST_ALERT_RECEIVED = "trustalert.received.{alertId}";

    public static final String PRETALX_API_KEY_INVALID = "pretalx.apikey.invalid.{apiKey}";


    public static @NonNull String format(@NonNull String template, @NonNull Map<String, ?> values) {
        var m = PLACEHOLDER.matcher(template);
        var sb = new StringBuilder(template.length());

        while (m.find()) {
            String key = m.group(1);
            Object value = values.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Missing value for {" + key + "} in: " + template);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
