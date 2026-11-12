package at.conrunner.eventcatalog.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import at.conrunner.eventcatalog.audit.CatalogAudit;
import at.conrunner.eventcatalog.event.EventRepository;
import at.conrunner.eventcatalog.error.EventCatalogConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {
    @Mock
    CatalogAudit audit;
    @Mock
    private EventRepository eventRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationService locationService;

    @Test
    void create_createsAMutableLocationForAnExistingEvent() {
        UUID eventId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(locationRepository.existsByEventIdAndNameIgnoreCase(eventId, "Main Hall")).thenReturn(false);
        when(locationRepository.save(any(LocationEntity.class))).thenAnswer(invocation -> {
            LocationEntity location = invocation.getArgument(0);
            location.setId(locationId);
            return location;
        });

        LocationDetails result = locationService.create(eventId, new LocationData(" Main Hall ", "Center stage", "https://example.test/hall"));

        ArgumentCaptor<LocationEntity> locationCaptor = ArgumentCaptor.forClass(LocationEntity.class);
        verify(locationRepository).save(locationCaptor.capture());
        LocationEntity saved = locationCaptor.getValue();
        assertEquals(eventId, saved.getEventId());
        assertEquals("Main Hall", saved.getName());
        assertFalse(saved.isReadOnly());
        assertEquals(locationId, result.id());
    }

    @Test
    void create_rejectsADuplicateLocationNameWithinTheSameEventIgnoringCase() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(locationRepository.existsByEventIdAndNameIgnoreCase(eventId, "main hall")).thenReturn(true);

        assertThrows(EventCatalogConflictException.class,
            () -> locationService.create(eventId, new LocationData("main hall", null, null)));
    }

    @Test
    void update_andDelete_rejectReadOnlyImportedLocations() {
        UUID locationId = UUID.randomUUID();
        LocationEntity importedLocation = new LocationEntity();
        importedLocation.setId(locationId);
        importedLocation.setEventId(UUID.randomUUID());
        importedLocation.setName("Imported location");
        importedLocation.setReadOnly(true);
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(importedLocation));

        assertThrows(EventCatalogConflictException.class,
            () -> locationService.update(locationId, new LocationData("Updated", null, null)));
        assertThrows(EventCatalogConflictException.class, () -> locationService.delete(locationId));
    }

    @Test
    void get_returnsTheStoredLocationWithoutExposingItsEntity() {
        UUID locationId = UUID.randomUUID();
        LocationEntity location = new LocationEntity();
        location.setId(locationId);
        location.setEventId(UUID.randomUUID());
        location.setName("Main Hall");
        location.setDescription("Center stage");
        location.setUrl("https://example.test/hall");
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));

        LocationDetails result = locationService.get(locationId);

        assertEquals(locationId, result.id());
        assertEquals("Main Hall", result.name());
    }
}
