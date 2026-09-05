package at.control.shiftcontrol.shift.positionslot;

import java.util.UUID;

public record PositionSlotDetails(UUID id, UUID shiftId, String name, String description, int desiredVolunteerCount,
                                  UUID requiredRoleId, boolean skipAutoAssignment) {
}
