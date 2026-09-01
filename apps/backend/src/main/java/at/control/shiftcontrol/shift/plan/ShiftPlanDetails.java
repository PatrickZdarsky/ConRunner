package at.control.shiftcontrol.shift.plan;

import java.time.Instant;
import java.util.UUID;

public record ShiftPlanDetails(UUID id, UUID eventId, String name, String shortDescription, String longDescription,
                               StaffingMode defaultStaffingMode, Instant createdAt, Instant archivedAt) {
}
