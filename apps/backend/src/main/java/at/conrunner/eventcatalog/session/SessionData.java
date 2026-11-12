package at.conrunner.eventcatalog.session;

import java.time.Instant;
import java.util.UUID;

import at.conrunner.eventcatalog.error.EventCatalogValidationException;

/** Validated session data accepted by the session service. */
public record SessionData(String name, String description, Instant startTime, Instant endTime, UUID locationId) {
    public SessionData {
        name = required(name, "Session name");
        description = optional(description);
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new EventCatalogValidationException("Session end time must be after start time");
        }
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
}
