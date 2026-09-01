package at.control.shiftcontrol.shift.positionslot;

import java.time.Instant;
import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.ShiftModeOverride;
import at.control.shiftcontrol.shift.plan.ShiftPlanData;

public record StaffingShiftData(String name, String shortDescription, String longDescription, Instant startTime, Instant endTime,
                                UUID locationId, UUID sessionId, ShiftModeOverride modeOverride) {
    public StaffingShiftData {
        name = ShiftPlanData.required(name, "Staffing shift name");
        shortDescription = ShiftPlanData.optional(shortDescription);
        longDescription = ShiftPlanData.optional(longDescription);
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ShiftValidationException("Staffing shift end time must be after start time");
        }
        if (locationId != null && sessionId != null) {
            throw new ShiftValidationException("A session-linked staffing shift uses the session location and cannot set a direct location");
        }
        modeOverride = modeOverride == null ? ShiftModeOverride.INHERIT : modeOverride;
    }
}
