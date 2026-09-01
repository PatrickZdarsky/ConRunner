package at.control.shiftcontrol.shift.plan;

/** API meaning for a shift's local staffing-mode setting. */
public enum ShiftModeOverride {
    INHERIT,
    SELF_SIGNUP,
    SUPERVISED,
    LOCKED;

    public StaffingMode explicitMode() {
        return this == INHERIT ? null : StaffingMode.valueOf(name());
    }

    public static ShiftModeOverride from(StaffingMode mode) {
        return mode == null ? INHERIT : ShiftModeOverride.valueOf(mode.name());
    }
}
