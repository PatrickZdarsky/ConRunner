package at.control.shiftcontrol.shift.positionslot;

import java.time.Instant;
import java.util.UUID;

import at.control.shiftcontrol.shift.plan.ShiftModeOverride;
import at.control.shiftcontrol.shift.plan.StaffingMode;

public record StaffingShiftDetails(UUID id, UUID planId, String name, String shortDescription, String longDescription,
                                   Instant startTime, Instant endTime, UUID locationId, UUID sessionId,
                                   ShiftModeOverride modeOverride, StaffingMode effectiveMode) {
}
