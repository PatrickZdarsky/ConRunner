package at.conrunner.schedule.error;

/** An event-catalogue resource requested by its stable ID does not exist. */
public class EventCatalogNotFoundException extends RuntimeException {
    public EventCatalogNotFoundException(String message) {
        super(message);
    }
}
