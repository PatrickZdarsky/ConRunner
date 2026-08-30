package at.control.shiftcontrol.eventcatalog.event;

import at.control.shiftcontrol.eventcatalog.error.EventCatalogValidationException;

/** An application value for a public event link; it is not an HTTP DTO. */
public record EventLink(EventLinkType type, String url) {
    public EventLink {
        if (type == null) {
            throw new EventCatalogValidationException("Event link type is required");
        }
        url = EventData.optionalAbsoluteUrl(url, "Event link URL");
        if (url == null) {
            throw new EventCatalogValidationException("Event link URL is required");
        }
    }
}
