package at.control.shiftcontrol.shift.lib.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import at.shiftcontrol.lib.exception.IllegalArgumentException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeUtil {
    private static final ZoneId VIENNA_ZONE_ID = ZoneId.of("Europe/Vienna");
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    /**
     * Returns the current date and time in Vienna converted to UTC.
     *
     * @return The current date and time in Vienna converted to UTC as a {@link Instant} object.
     */
    public static Instant getCurrentViennaTimeInUtc() {
        return ZonedDateTime.now(VIENNA_ZONE_ID).withZoneSameInstant(UTC_ZONE_ID).toInstant();
    }

    /**
     * Creates a {@link Instant} object with the given date and time in Vienna and converts it to UTC.
     *
     * @return The {@link Instant} object with the given date and time in Vienna converted to UTC.
     */
    public static Instant createViennaTimeInUtc(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, 0, VIENNA_ZONE_ID).withZoneSameInstant(UTC_ZONE_ID).toInstant();
    }

    public static LocalDate convertToUtcLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, UTC_ZONE_ID).toLocalDate();
    }

    public static Instant convertToStartOfUtcDayInstant(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(UTC_ZONE_ID).toInstant();
    }

    public static Instant convertToEndOfUtcDayInstant(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atTime(23, 59, 59, 999_999_999).atZone(UTC_ZONE_ID).toInstant();
    }

    public static long calculateDurationInMinutes(Instant startInstant, Instant endInstant) {
        long durationInMillis = calculateDurationInMillis(startInstant, endInstant);

        return durationInMillis / (1000 * 60);
    }

    public static long calculateDurationInMinutesCeil(Instant startInstant, Instant endInstant) {
        long durationInMillis = calculateDurationInMillis(startInstant, endInstant);

        // CEIL: any started minute counts as a full minute
        return (durationInMillis + (60_000 - 1)) / 60_000;
    }

    private static long calculateDurationInMillis(Instant startInstant, Instant endInstant) {
        if (startInstant == null || endInstant == null) {
            throw new IllegalArgumentException("shift start/end must not be null");
        }
        if (!endInstant.isAfter(startInstant)) {
            throw new IllegalArgumentException("shift end must be after start");
        }
        return endInstant.toEpochMilli() - startInstant.toEpochMilli();
    }

    /**
     * Formats the given {@link Instant} object to a string with the pattern "dd.MM.yyyy HH:mm:ss.SSS".
     *
     * @param instant The {@link Instant} object to format.
     * @return The formatted string.
     */
    public static String formatInstant(Instant instant) {
        if (instant == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");
        return instant.atZone(UTC_ZONE_ID).format(formatter);
    }
}
