package at.conrunner.schedule.error;

/** A requested change conflicts with the current event-catalogue state. */
public class EventCatalogConflictException extends RuntimeException {
    public EventCatalogConflictException(String message) {
        super(message);
    }
}
