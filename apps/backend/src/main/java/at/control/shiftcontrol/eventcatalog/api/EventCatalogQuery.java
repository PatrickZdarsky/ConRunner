package at.control.shiftcontrol.eventcatalog.api;

import java.time.Instant;
import java.util.UUID;

/** Read-only catalogue facts needed by staffing. */
public interface EventCatalogQuery {
    CatalogEvent requireEvent(UUID eventId);

    CatalogLocation requireLocation(UUID locationId);

    CatalogSession requireSession(UUID sessionId);

    record CatalogEvent(UUID id, Instant startTime, Instant endTime) {
    }

    record CatalogLocation(UUID id, UUID eventId) {
    }

    record CatalogSession(UUID id, UUID eventId, UUID locationId) {
    }
}
