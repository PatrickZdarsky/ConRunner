package at.conrunner.communications.api;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Small, versioned snapshot of the context needed to render a notification. */
public record NotificationContext(String target, String subject, String detail) {
    private static final String PREFIX = "v1.";

    public static String encode(String target, String subject, String detail) {
        return PREFIX + encodeField(target) + "." + encodeField(clean(subject)) + "." + encodeField(clean(detail));
    }

    private static String clean(String value) {
        if (value == null) return "";
        var cleaned = value.replaceAll("\\p{Cc}", " ").strip();
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    public static NotificationContext decode(String data) {
        if (data == null || !data.startsWith(PREFIX)) return new NotificationContext(data, "", "");
        var fields = data.substring(PREFIX.length()).split("\\.", -1);
        if (fields.length != 3) throw new IllegalArgumentException("Invalid notification context");
        return new NotificationContext(decodeField(fields[0]), decodeField(fields[1]), decodeField(fields[2]));
    }

    private static String encodeField(String value) {
        if (value == null) value = "";
        if (value.length() > 255 || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid notification context field");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String value) {
        if (value.length() > 1360) throw new IllegalArgumentException("Invalid notification context field");
        var decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        encodeField(decoded);
        return decoded;
    }
}
