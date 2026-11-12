package at.conrunner.shift.lib.type;

/**
 * Represents different types of time constraints that can be applied to shifts.
 *
 * <p>
 * Currently only {@link #UNAVAILABLE} is used, but this enum is intended to be
 * extensible as additional time constraint types are introduced.
 */
public enum TimeConstraintType {
    UNAVAILABLE,
    EMERGENCY
}
