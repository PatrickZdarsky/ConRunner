package at.control.shiftcontrol.eventcatalog.error;

/** A requested change conflicts with the current event-catalogue state. */
public class EventCatalogConflictException extends RuntimeException {
    public EventCatalogConflictException(String message) {
        super(message);
    }
}
