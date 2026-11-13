package at.conrunner.schedule.event;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import at.conrunner.platform.exception.ValidationException;

/** Validated event data accepted by the event-catalogue service. */
public record EventData(
    String name,
    String shortDescription,
    String longDescription,
    Instant startTime,
    Instant endTime,
    String rewardPointsRedeemUrl,
    List<EventLink> links
) {
    public EventData {
        name = required(name, "Event name");
        shortDescription = optional(shortDescription);
        longDescription = optional(longDescription);
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ValidationException("Event end time must be after start time");
        }
        rewardPointsRedeemUrl = optionalAbsoluteUrl(rewardPointsRedeemUrl, "Reward points redeem URL");
        links = uniqueLinks(links);
    }

    static String required(String value, String label) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new ValidationException(label + " is required");
        }
        return normalized;
    }

    static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static String optionalAbsoluteUrl(String value, String label) {
        String normalized = optional(value);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException _) {
            throw new ValidationException(label + " must be a valid absolute URL");
        }
        return normalized;
    }

    private static List<EventLink> uniqueLinks(List<EventLink> links) {
        if (links == null || links.isEmpty()) {
            return List.of();
        }
        if (links.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ValidationException("Event links cannot contain null entries");
        }
        return List.copyOf(new LinkedHashSet<>(links));
    }
}
