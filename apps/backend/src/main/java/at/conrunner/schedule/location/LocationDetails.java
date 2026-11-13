package at.conrunner.schedule.location;

import java.util.UUID;

/** Immutable location data returned by the location application service. */
public record LocationDetails(UUID id, UUID eventId, String name, String description, String url, boolean readOnly) {
}
