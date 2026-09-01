package at.control.shiftcontrol.shift.plan;

import at.control.shiftcontrol.shift.error.ShiftValidationException;

public record ShiftPlanData(String name, String shortDescription, String longDescription) {
    public ShiftPlanData {
        name = required(name, "Shift plan name");
        shortDescription = optional(shortDescription);
        longDescription = optional(longDescription);
    }

    public static String required(String value, String label) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new ShiftValidationException(label + " is required");
        }
        return normalized;
    }

    public static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
