package at.conrunner.platform.customfields.api;

/** Domain records that can own custom-field values. */
public enum FieldEntityType {
    /** Event catalog event, which supplies its own event scope. */
    EVENT,
    /** Session scoped to its parent event. */
    SESSION,
    /** Location scoped to its parent event. */
    LOCATION,
    /** Shift scoped to the event of its shift plan. */
    SHIFT
}
