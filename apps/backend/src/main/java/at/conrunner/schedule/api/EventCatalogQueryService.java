package at.conrunner.schedule.api;

import java.util.UUID;

import at.conrunner.platform.exception.NotFoundException;
import at.conrunner.schedule.event.EventRepository;
import at.conrunner.schedule.location.LocationRepository;
import at.conrunner.schedule.session.SessionRepository;
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
            throw new NotFoundException("Event not found");
        }
        return eventRepository.findById(eventId)
            .map(event -> new CatalogEvent(event.getId(), event.getStartTime(), event.getEndTime()))
            .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
    }

    @Override
    public CatalogLocation requireLocation(UUID locationId) {
        if (locationId == null) {
            throw new NotFoundException("Location not found");
        }
        return locationRepository.findById(locationId)
            .map(location -> new CatalogLocation(location.getId(), location.getEventId()))
            .orElseThrow(() -> new NotFoundException("Location not found: " + locationId));
    }

    @Override
    public CatalogSession requireSession(UUID sessionId) {
        if (sessionId == null) {
            throw new NotFoundException("Session not found");
        }
        return sessionRepository.findById(sessionId)
            .map(session -> new CatalogSession(session.getId(), session.getEventId(), session.getLocationId()))
            .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
    }
}
