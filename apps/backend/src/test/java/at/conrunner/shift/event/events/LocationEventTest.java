package at.conrunner.shift.event.events;

import at.conrunner.shift.entity.Location;
import at.conrunner.shift.lib.event.events.LocationEvent;
import at.conrunner.shift.lib.event.events.parts.LocationPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class LocationEventTest {

    @Test
    void ofInternal() {
        Location location = mock(Location.class);
        String routingKey = "routingKey";

        LocationPart locationPart = mock(LocationPart.class);
        try (var locationPartMock = org.mockito.Mockito.mockStatic(LocationPart.class)) {
            locationPartMock.when(() -> LocationPart.of(location)).thenReturn(locationPart);

            LocationEvent locationEvent = LocationEvent.ofInternal(null, routingKey, location);

            assertEquals(locationPart, locationEvent.getLocation());
            assertEquals(routingKey, locationEvent.getRoutingKey());
        }
    }
}

