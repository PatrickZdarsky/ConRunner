package at.control.shiftcontrol.shift;

import java.time.Instant;
import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.ShiftModeOverride;
import at.control.shiftcontrol.shift.plan.ShiftPlanData;

public record ShiftData(String name, String shortDescription, String longDescription, Instant startTime, Instant endTime,
                        UUID locationId, UUID sessionId, ShiftModeOverride modeOverride) {
    public ShiftData {
        name = ShiftPlanData.required(name, "Shift name");
        shortDescription = ShiftPlanData.optional(shortDescription);
        longDescription = ShiftPlanData.optional(longDescription);
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ShiftValidationException("Shift end time must be after start time");
        }
        if (locationId != null && sessionId != null) {
            throw new ShiftValidationException("A session-linked shift uses its session location and cannot set a direct location");
        }
        modeOverride = modeOverride == null ? ShiftModeOverride.INHERIT : modeOverride;
    }
}
