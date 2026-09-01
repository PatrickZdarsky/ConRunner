package at.control.shiftcontrol.eventcatalog.session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.event.CatalogEventEntity;
import at.control.shiftcontrol.eventcatalog.event.EventRepository;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogConflictException;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogNotFoundException;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogValidationException;
import at.control.shiftcontrol.eventcatalog.location.LocationEntity;
import at.control.shiftcontrol.eventcatalog.location.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SessionService {
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final SessionRepository sessionRepository;

    public SessionService(EventRepository eventRepository, LocationRepository locationRepository, SessionRepository sessionRepository) {
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public SessionDetails create(UUID eventId, SessionData data) {
        CatalogEventEntity event = getEvent(eventId);
        if (data == null) {
            throw new EventCatalogValidationException("Session data is required");
        }
        validateEventAndLocation(event, data);
        assertNameAvailable(eventId, data.name(), null);

        SessionEntity session = new SessionEntity();
        session.setEventId(eventId);
        session.setReadOnly(false);
        apply(session, data);
        return toDetails(sessionRepository.save(session));
    }

    @Transactional
    public SessionDetails update(UUID sessionId, SessionData data) {
        SessionEntity session = getEntity(sessionId);
        if (session.isReadOnly()) {
            throw new EventCatalogConflictException("Cannot modify a read-only imported session");
        }
        if (data == null) {
            throw new EventCatalogValidationException("Session data is required");
        }
        validateEventAndLocation(getEvent(session.getEventId()), data);
        assertNameAvailable(session.getEventId(), data.name(), session);
        apply(session, data);
        return toDetails(sessionRepository.save(session));
    }

    public SessionDetails get(UUID sessionId) {
        return toDetails(getEntity(sessionId));
    }

    public List<SessionDetails> findAllForEvent(UUID eventId) {
        requireEvent(eventId);
        return sessionRepository.findAllByEventIdOrderByStartTimeAsc(eventId).stream().map(SessionService::toDetails).toList();
    }

    @Transactional
    public void delete(UUID sessionId) {
        SessionEntity session = getEntity(sessionId);
        if (session.isReadOnly()) {
            throw new EventCatalogConflictException("Cannot modify a read-only imported session");
        }
        sessionRepository.delete(session);
    }

    public List<SessionDetails> suggest(UUID eventId, SessionSuggestion suggestion) {
        requireEvent(eventId);
        return sessionRepository.findAllByEventIdOrderByStartTimeAsc(eventId).stream()
            .filter(session -> matchesName(session, suggestion))
            .filter(session -> overlaps(session, suggestion))
            .map(SessionService::toDetails)
            .toList();
    }

    private CatalogEventEntity getEvent(UUID eventId) {
        if (eventId == null) {
            throw new EventCatalogValidationException("Event ID is required");
        }
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new EventCatalogNotFoundException("Event not found: " + eventId));
    }

    private void requireEvent(UUID eventId) {
        if (eventId == null || !eventRepository.existsById(eventId)) {
            throw new EventCatalogNotFoundException("Event not found: " + eventId);
        }
    }

    private SessionEntity getEntity(UUID sessionId) {
        if (sessionId == null) {
            throw new EventCatalogValidationException("Session ID is required");
        }
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new EventCatalogNotFoundException("Session not found: " + sessionId));
    }

    private void validateEventAndLocation(CatalogEventEntity event, SessionData data) {
        if (data.startTime().isBefore(event.getStartTime()) || data.endTime().isAfter(event.getEndTime())) {
            throw new EventCatalogValidationException("Session time range must be within event time range");
        }
        if (data.locationId() != null) {
            LocationEntity location = locationRepository.findById(data.locationId())
                .orElseThrow(() -> new EventCatalogNotFoundException("Location not found: " + data.locationId()));
            if (!event.getId().equals(location.getEventId())) {
                throw new EventCatalogValidationException("Session location must belong to the same event");
            }
        }
    }

    private void assertNameAvailable(UUID eventId, String name, SessionEntity currentSession) {
        if (currentSession != null && currentSession.getName().equalsIgnoreCase(name)) {
            return;
        }
        if (sessionRepository.existsByEventIdAndNameIgnoreCase(eventId, name)) {
            throw new EventCatalogConflictException("Session name must be unique within an event");
        }
    }

    private static void apply(SessionEntity session, SessionData data) {
        session.setName(data.name());
        session.setDescription(data.description());
        session.setStartTime(data.startTime());
        session.setEndTime(data.endTime());
        session.setLocationId(data.locationId());
    }

    private static boolean matchesName(SessionEntity session, SessionSuggestion suggestion) {
        return suggestion == null || suggestion.name() == null || suggestion.name().isBlank()
            || session.getName().toLowerCase().contains(suggestion.name().trim().toLowerCase());
    }

    private static boolean overlaps(SessionEntity session, SessionSuggestion suggestion) {
        if (suggestion == null || suggestion.startTime() == null) {
            return true;
        }
        long toleranceSeconds = suggestion.toleranceInMinutes() * 60L;
        return !session.getEndTime().isBefore(suggestion.startTime().minusSeconds(toleranceSeconds))
            && !session.getStartTime().isAfter(suggestion.endTime().plusSeconds(toleranceSeconds));
    }

    private static SessionDetails toDetails(SessionEntity session) {
        return new SessionDetails(session.getId(), session.getEventId(), session.getLocationId(), session.getName(), session.getDescription(),
            session.getStartTime(), session.getEndTime(), session.isReadOnly());
    }

}
