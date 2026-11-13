package at.conrunner.schedule.event;

import at.conrunner.platform.exception.ValidationException;

/** An application value for a public event link; it is not an HTTP DTO. */
public record EventLink(EventLinkType type, String url) {
    public EventLink {
        if (type == null) {
            throw new ValidationException("Event link type is required");
        }
        url = EventData.optionalAbsoluteUrl(url, "Event link URL");
        if (url == null) {
            throw new ValidationException("Event link URL is required");
        }
    }
}
