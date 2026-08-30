package at.control.shiftcontrol.eventcatalog.error;

/** A caller supplied data that violates an event-catalogue business rule. */
public class EventCatalogValidationException extends RuntimeException {
    public EventCatalogValidationException(String message) {
        super(message);
    }
}
