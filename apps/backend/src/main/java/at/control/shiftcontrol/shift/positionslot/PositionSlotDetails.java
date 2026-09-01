package at.control.shiftcontrol.shift.positionslot;

import java.util.UUID;

public record PositionSlotDetails(UUID id, UUID staffingShiftId, String name, String description, int desiredVolunteerCount,
                                  UUID requiredRoleId, boolean skipAutoAssignment) {
}
