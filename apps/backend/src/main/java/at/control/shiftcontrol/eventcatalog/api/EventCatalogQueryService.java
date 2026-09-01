package at.control.shiftcontrol.eventcatalog.api;

import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.error.EventCatalogNotFoundException;
import at.control.shiftcontrol.eventcatalog.event.EventRepository;
import at.control.shiftcontrol.eventcatalog.location.LocationRepository;
import at.control.shiftcontrol.eventcatalog.session.SessionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class EventCatalogQueryService implements EventCatalogQuery {
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final SessionRepository sessionRepository;

    @Override
    public CatalogEvent requireEvent(UUID eventId) {
        if (eventId == null) {
            throw new EventCatalogNotFoundException("Event not found");
        }
        return eventRepository.findById(eventId)
            .map(event -> new CatalogEvent(event.getId(), event.getStartTime(), event.getEndTime()))
            .orElseThrow(() -> new EventCatalogNotFoundException("Event not found: " + eventId));
    }

    @Override
    public CatalogLocation requireLocation(UUID locationId) {
        if (locationId == null) {
            throw new EventCatalogNotFoundException("Location not found");
        }
        return locationRepository.findById(locationId)
            .map(location -> new CatalogLocation(location.getId(), location.getEventId()))
            .orElseThrow(() -> new EventCatalogNotFoundException("Location not found: " + locationId));
    }

    @Override
    public CatalogSession requireSession(UUID sessionId) {
        if (sessionId == null) {
            throw new EventCatalogNotFoundException("Session not found");
        }
        return sessionRepository.findById(sessionId)
            .map(session -> new CatalogSession(session.getId(), session.getEventId(), session.getLocationId()))
            .orElseThrow(() -> new EventCatalogNotFoundException("Session not found: " + sessionId));
    }
}
