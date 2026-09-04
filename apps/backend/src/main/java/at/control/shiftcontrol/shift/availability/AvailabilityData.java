package at.control.shiftcontrol.shift.availability;

import java.time.Instant;

import at.control.shiftcontrol.shift.error.ShiftValidationException;

/** A normal, time-bounded declaration that a volunteer is unavailable. */
public record AvailabilityData(Instant startTime, Instant endTime, String note) {
    public AvailabilityData {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ShiftValidationException("Availability end time must be after start time");
        }
        note = note == null || note.isBlank() ? null : note.trim();
        if (note != null && note.length() > 1024) {
            throw new ShiftValidationException("Availability note must be at most 1024 characters");
        }
    }
}
