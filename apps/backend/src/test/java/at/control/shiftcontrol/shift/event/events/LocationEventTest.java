package at.control.shiftcontrol.shift.event.events;

import at.control.shiftcontrol.shift.entity.Location;
import at.control.shiftcontrol.shift.lib.event.events.LocationEvent;
import at.control.shiftcontrol.shift.lib.event.events.parts.LocationPart;
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

