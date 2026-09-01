package at.control.shiftcontrol.shift.positionslot;

import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.ShiftPlanData;

public record PositionSlotData(String name, String description, int desiredVolunteerCount, UUID requiredRoleId,
                               boolean skipAutoAssignment) {
    public PositionSlotData {
        name = ShiftPlanData.required(name, "Position slot name");
        description = ShiftPlanData.optional(description);
        if (desiredVolunteerCount <= 0) {
            throw new ShiftValidationException("Desired volunteer count must be positive");
        }
    }
}
