package at.control.shiftcontrol.shift;

import java.time.Duration;
import java.time.Instant;

import at.control.shiftcontrol.shift.error.ShiftValidationException;

public record TimeConstraintData(TimeConstraintType type, Instant startTime, Instant endTime, String note) {
    public TimeConstraintData {
        if (type == null) {
            throw new ShiftValidationException("Time constraint type is required");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ShiftValidationException("Time constraint end time must be after start time");
        }
        if (type == TimeConstraintType.EMERGENCY
            && Duration.between(startTime, endTime).toSeconds() % Duration.ofDays(1).toSeconds() != 0) {
            throw new ShiftValidationException("Emergency time constraints must span a positive multiple of 24 hours");
        }
        note = note == null || note.isBlank() ? null : note.trim();
        if (note != null && note.length() > 1024) {
            throw new ShiftValidationException("Time constraint note must be at most 1024 characters");
        }
    }
}
