package at.control.shiftcontrol.communications.request;

import at.control.shiftcontrol.communications.api.NotificationContext;
import at.control.shiftcontrol.communications.api.NotificationTypes;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/** Stable type IDs select translated messages; domain modules supply only safe context. */
public final class NotificationTemplates {
    public record Template(String title, String body) {}
    private static final List<String> TYPES = List.of(
        NotificationTypes.TRADE_REQUESTED, NotificationTypes.TRADE_ACCEPTED, NotificationTypes.TRADE_DECLINED,
        NotificationTypes.ROLE_ASSIGNED, NotificationTypes.ROLE_UNASSIGNED,
        NotificationTypes.ASSIGNMENT_JOIN_REQUESTED, NotificationTypes.ASSIGNMENT_LEAVE_REQUESTED,
        NotificationTypes.ASSIGNMENT_JOIN_ACCEPTED, NotificationTypes.ASSIGNMENT_JOIN_DECLINED,
        NotificationTypes.ASSIGNMENT_LEAVE_ACCEPTED, NotificationTypes.ASSIGNMENT_LEAVE_DECLINED,
        NotificationTypes.MEMBERSHIP_CHANGED, NotificationTypes.ROLES_CHANGED,
        NotificationTypes.VOLUNTEER_LOCKED, NotificationTypes.VOLUNTEER_UNLOCKED, NotificationTypes.VOLUNTEER_RESET,
        NotificationTypes.AUCTION_CLAIMED, NotificationTypes.PLAN_VOLUNTEER_JOINED,
        NotificationTypes.PLAN_PLANNER_JOINED, NotificationTypes.TRUST_ALERT);
    private static final ResourceBundle.Control CONTROL = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);
    private NotificationTemplates() {}

    public static Template get(String type) { return get(type, Locale.ENGLISH); }
    public static Template get(String type, Locale locale) { return render(type, new NotificationContext("", "", ""), locale); }
    public static Template render(String type, NotificationContext context) { return render(type, context, Locale.ENGLISH); }

    public static Template render(String type, NotificationContext context, Locale locale) {
        if (!supports(type)) throw new IllegalArgumentException("Unsupported notification type");
        var effectiveLocale = locale == null ? Locale.ENGLISH : locale;
        var bundleName = "en-x-uwu".equalsIgnoreCase(effectiveLocale.toLanguageTag())
            ? "notification-messages-uwu" : "notification-messages";
        var bundle = ResourceBundle.getBundle(bundleName, effectiveLocale, CONTROL);
        String bodyKey = type + ".body";
        var subject = context.subject();
        var detail = context.detail();
        if ((type.equals(NotificationTypes.MEMBERSHIP_CHANGED) || type.equals(NotificationTypes.ROLES_CHANGED))
            && ("added".equals(detail) || "removed".equals(detail))) bodyKey = type + "." + detail;
        else if (subject != null && !subject.isBlank() && bundle.containsKey(type + ".context")
            && (!type.equals(NotificationTypes.TRADE_REQUESTED) || (detail != null && !detail.isBlank()))) bodyKey = type + ".context";
        return new Template(bundle.getString(type + ".title"),
            new MessageFormat(bundle.getString(bodyKey), effectiveLocale).format(new Object[]{subject, detail}));
    }
    public static boolean supports(String type) { return type != null && TYPES.contains(type); }
    public static List<String> types() { return TYPES.stream().sorted().toList(); }
}
