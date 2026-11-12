package at.conrunner.eventcatalog.session;

import java.time.Instant;
import java.util.UUID;

/** Immutable session data returned by the session application service. */
public record SessionDetails(UUID id, UUID eventId, UUID locationId, String name, String description, Instant startTime, Instant endTime,
                             boolean readOnly) {
}
