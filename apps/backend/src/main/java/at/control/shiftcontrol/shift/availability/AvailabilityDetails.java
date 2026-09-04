package at.control.shiftcontrol.shift.availability;

import java.time.Instant;
import java.util.UUID;

/** Read model deliberately includes the authorized exception that permitted a conflict. */
public record AvailabilityDetails(UUID id, UUID eventId, UUID userId, Instant startTime, Instant endTime, String note,
                                  UUID createdByUserId, String authorizedOverrideReason, Instant createdAt) {
}
