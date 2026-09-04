package at.control.shiftcontrol.shift.assignment;

import java.time.Instant;
import java.util.UUID;

public record AssignmentDetails(UUID id, UUID positionSlotId, UUID volunteerId, AssignmentState state,
                                String authorizedOverrideReason, Instant createdAt, Instant updatedAt) {
}
