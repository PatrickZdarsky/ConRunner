package at.control.shiftcontrol.shift;

import java.time.Instant;
import java.util.UUID;

public record TimeConstraintDetails(UUID id, UUID eventId, UUID userId, TimeConstraintType type,
                                    Instant startTime, Instant endTime, String note, Instant createdAt) {
}
