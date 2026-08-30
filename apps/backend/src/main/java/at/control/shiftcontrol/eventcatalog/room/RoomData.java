package at.control.shiftcontrol.eventcatalog.room;

import java.net.URI;

import at.control.shiftcontrol.eventcatalog.error.EventCatalogValidationException;

/** Validated room data accepted by the room service. */
public record RoomData(String name, String description, String url) {
    public RoomData {
        name = required(name, "Room name");
        description = optional(description);
        url = optionalAbsoluteUrl(url);
    }

    private static String required(String value, String label) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new EventCatalogValidationException(label + " is required");
        }
        return normalized;
    }

    private static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String optionalAbsoluteUrl(String value) {
        String normalized = optional(value);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new EventCatalogValidationException("Room URL must be a valid absolute URL");
        }
        return normalized;
    }
}
