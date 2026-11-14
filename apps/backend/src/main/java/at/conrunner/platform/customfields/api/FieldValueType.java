package at.conrunner.platform.customfields.api;

/** JSON shape of a scalar field value or of each item in a multiple field. */
public enum FieldValueType {
    /** JSON string. Required text must contain a non-whitespace character. */
    TEXT,
    /** Absolute HTTP(S) URL string without credentials. */
    URL,
    /** Finite JSON number. */
    NUMBER,
    /** ISO local date string. */
    DATE,
    /** ISO datetime string with an explicit offset. */
    DATETIME,
    /** Canonical UUID string for a user the owner may reference. */
    USER,
    /** JSON object containing only a nonblank {@code label} string and a {@code url}. */
    LINK
}
