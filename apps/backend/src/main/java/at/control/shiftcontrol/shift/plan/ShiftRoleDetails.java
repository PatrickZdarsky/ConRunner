package at.control.shiftcontrol.shift.plan;

import java.util.UUID;

public record ShiftRoleDetails(UUID id, UUID planId, String name, String description, boolean selfAssignable) {
}
