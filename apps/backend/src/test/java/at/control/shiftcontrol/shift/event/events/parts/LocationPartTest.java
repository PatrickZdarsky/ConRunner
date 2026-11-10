package at.control.shiftcontrol.shift.event.events.parts;

import at.control.shiftcontrol.shift.entity.Location;
import at.control.shiftcontrol.shift.lib.event.events.parts.LocationPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LocationPartTest {

    @Test
    void of() {
        // Arrange
        Location location = new Location();
        location.setId(1L);
        location.setName("Test Location");
        location.setDescription("Test Description");
        location.setUrl("http://test.com");
        location.setReadOnly(true);

        // Act
        LocationPart locationPart = LocationPart.of(location);

        // Assert
        assertEquals(String.valueOf(location.getId()), locationPart.getId());
        assertEquals(location.getName(), locationPart.getName());
        assertEquals(location.getDescription(), locationPart.getDescription());
        assertEquals(location.getUrl(), locationPart.getUrl());
        assertEquals(location.isReadOnly(), locationPart.isReadOnly());
    }

    @Test
    void of_throwsNullPointerException_forNullLocation() {
        assertThrows(NullPointerException.class, () -> {
            LocationPart.of(null);
        });
    }
}

