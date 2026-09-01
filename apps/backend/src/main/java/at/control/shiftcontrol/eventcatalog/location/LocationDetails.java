package at.control.shiftcontrol.eventcatalog.location;

import java.util.UUID;

/** Immutable location data returned by the location application service. */
public record LocationDetails(UUID id, UUID eventId, String name, String description, String url, boolean readOnly) {
}
