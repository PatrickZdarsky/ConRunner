package at.conrunner.eventcatalog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import at.conrunner.eventcatalog.error.EventCatalogNotFoundException;
import at.conrunner.eventcatalog.event.CatalogEventEntity;
import at.conrunner.eventcatalog.event.EventRepository;
import at.conrunner.eventcatalog.location.LocationEntity;
import at.conrunner.eventcatalog.location.LocationRepository;
import at.conrunner.eventcatalog.session.SessionEntity;
import at.conrunner.eventcatalog.session.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventCatalogQueryServiceTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private EventCatalogQueryService query;

    @Test
    void exposesOnlyTheStableCatalogueReferencesNeededByOtherModules() {
        UUID eventId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        CatalogEventEntity event = new CatalogEventEntity();
        event.setId(eventId);
        event.setStartTime(Instant.parse("2026-05-12T07:00:00Z"));
        event.setEndTime(Instant.parse("2026-05-12T22:00:00Z"));
        LocationEntity location = new LocationEntity();
        location.setId(locationId);
        location.setEventId(eventId);
        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        session.setEventId(eventId);
        session.setLocationId(locationId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertEquals(eventId, query.requireEvent(eventId).id());
        assertEquals(event.getEndTime(), query.requireEvent(eventId).endTime());
        assertEquals(eventId, query.requireLocation(locationId).eventId());
        assertEquals(locationId, query.requireSession(sessionId).locationId());
    }

    @Test
    void rejectsMissingCatalogueReferences() {
        assertThrows(EventCatalogNotFoundException.class, () -> query.requireEvent(null));
        assertThrows(EventCatalogNotFoundException.class, () -> query.requireLocation(UUID.randomUUID()));
        assertThrows(EventCatalogNotFoundException.class, () -> query.requireSession(UUID.randomUUID()));
    }
}
