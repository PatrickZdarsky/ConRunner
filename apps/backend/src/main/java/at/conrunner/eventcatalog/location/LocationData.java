package at.conrunner.eventcatalog.location;

import java.net.URI;

import at.conrunner.eventcatalog.error.EventCatalogValidationException;

/** Validated location data accepted by the location service. */
public record LocationData(String name, String description, String url) {
    public LocationData {
        name = required(name, "Location name");
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
            throw new EventCatalogValidationException("Location URL must be a valid absolute URL");
        }
        return normalized;
    }
}
