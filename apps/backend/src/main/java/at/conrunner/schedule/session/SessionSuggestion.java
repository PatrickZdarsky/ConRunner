package at.conrunner.schedule.session;

import java.time.Instant;

import at.conrunner.platform.exception.ValidationException;

/** Optional title and time-window filters for suggesting sessions while staffing is planned. */
public record SessionSuggestion(String name, Instant startTime, Instant endTime, int toleranceInMinutes) {
    public SessionSuggestion {
        boolean hasStart = startTime != null;
        boolean hasEnd = endTime != null;
        if (hasStart != hasEnd) {
            throw new ValidationException("Session suggestion requires both start and end time");
        }
        if (hasStart && !endTime.isAfter(startTime)) {
            throw new ValidationException("Session suggestion end time must be after start time");
        }
        if (toleranceInMinutes < 0) {
            throw new ValidationException("Session suggestion tolerance cannot be negative");
        }
    }
}
