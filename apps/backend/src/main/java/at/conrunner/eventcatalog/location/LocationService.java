package at.conrunner.eventcatalog.location;

import java.util.List;
import at.conrunner.eventcatalog.audit.CatalogAudit;
import java.util.UUID;

import at.conrunner.eventcatalog.event.EventRepository;
import at.conrunner.eventcatalog.error.EventCatalogConflictException;
import at.conrunner.eventcatalog.error.EventCatalogNotFoundException;
import at.conrunner.eventcatalog.error.EventCatalogValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LocationService {
    private final CatalogAudit audit;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;

    public LocationService(EventRepository eventRepository, LocationRepository locationRepository, CatalogAudit audit) {
        this.audit = audit;
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public LocationDetails create(UUID eventId, LocationData data) {
        requireEvent(eventId);
        if (data == null) {
            throw new EventCatalogValidationException("Location data is required");
        }
        assertNameAvailable(eventId, data.name(), null);

        LocationEntity location = new LocationEntity();
        location.setEventId(eventId);
        location.setReadOnly(false);
        apply(location, data);
        location = locationRepository.save(location);
        audit.record("location", "created", location.getId(), null, snapshot(location));
        return toDetails(location);
    }

    @Transactional
    public LocationDetails update(UUID locationId, LocationData data) {
        LocationEntity location = getEntity(locationId);
        assertMutable(location);
        if (data == null) {
            throw new EventCatalogValidationException("Location data is required");
        }
        assertNameAvailable(location.getEventId(), data.name(), location);
        var before = snapshot(location);
        apply(location, data);
        location = locationRepository.save(location);
        audit.record("location", "updated", location.getId(), before, snapshot(location));
        return toDetails(location);
    }

    public LocationDetails get(UUID locationId) {
        return toDetails(getEntity(locationId));
    }

    public List<LocationDetails> findAllForEvent(UUID eventId) {
        requireEvent(eventId);
        return locationRepository.findAllByEventIdOrderByNameAsc(eventId).stream().map(LocationService::toDetails).toList();
    }

    @Transactional
    public void delete(UUID locationId) {
        LocationEntity location = getEntity(locationId);
        assertMutable(location);
        audit.record("location", "deleted", location.getId(), snapshot(location), null);
        locationRepository.delete(location);
    }

    private void requireEvent(UUID eventId) {
        if (eventId == null || !eventRepository.existsById(eventId)) {
            throw new EventCatalogNotFoundException("Event not found: " + eventId);
        }
    }

    private LocationEntity getEntity(UUID locationId) {
        if (locationId == null) {
            throw new EventCatalogValidationException("Location ID is required");
        }
        return locationRepository.findById(locationId)
            .orElseThrow(() -> new EventCatalogNotFoundException("Location not found: " + locationId));
    }

    private void assertNameAvailable(UUID eventId, String name, LocationEntity currentLocation) {
        if (currentLocation != null && currentLocation.getName().equalsIgnoreCase(name)) {
            return;
        }
        if (locationRepository.existsByEventIdAndNameIgnoreCase(eventId, name)) {
            throw new EventCatalogConflictException("Location name must be unique within an event");
        }
    }

    private static CatalogAudit.Snapshot snapshot(LocationEntity e) {
        return new CatalogAudit.Snapshot(e.getName(), null, null, CatalogAudit.value(e.getEventId()), null);
    }

    private static void apply(LocationEntity location, LocationData data) {
        location.setName(data.name());
        location.setDescription(data.description());
        location.setUrl(data.url());
    }

    private static void assertMutable(LocationEntity location) {
        if (location.isReadOnly()) {
            throw new EventCatalogConflictException("Cannot modify a read-only imported location");
        }
    }

    private static LocationDetails toDetails(LocationEntity location) {
        return new LocationDetails(location.getId(), location.getEventId(), location.getName(), location.getDescription(), location.getUrl(),
            location.isReadOnly());
    }

}
