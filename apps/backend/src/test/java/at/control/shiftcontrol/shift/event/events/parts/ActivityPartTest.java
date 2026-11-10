package at.control.shiftcontrol.shift.event.events.parts;

import at.control.shiftcontrol.shift.entity.Activity;
import at.control.shiftcontrol.shift.entity.Location;
import at.control.shiftcontrol.shift.lib.event.events.parts.ActivityPart;
import at.control.shiftcontrol.shift.lib.event.events.parts.LocationPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityPartTest {

    @Test
    void of_withLocation() {
        // Arrange
        Location location = new Location();
        location.setId(2L);
        location.setName("Test Location");
        location.setDescription("Test Description");
        location.setUrl("http://test.com");
        location.setReadOnly(false);

        Activity activity = new Activity();
        activity.setId(1L);
        activity.setName("Test Activity");
        activity.setDescription("Test Description");
        activity.setStartTime(Instant.parse("2023-01-01T10:00:00Z"));
        activity.setEndTime(Instant.parse("2023-01-01T12:00:00Z"));
        activity.setLocation(location);
        activity.setReadOnly(true);

        // Act
        ActivityPart activityPart = ActivityPart.of(activity);

        // Assert
        assertEquals(String.valueOf(activity.getId()), activityPart.getId());
        assertEquals(activity.getName(), activityPart.getName());
        assertEquals(activity.getDescription(), activityPart.getDescription());
        assertEquals(activity.getStartTime(), activityPart.getStartTime());
        assertEquals(activity.getEndTime(), activityPart.getEndTime());
        assertTrue(activityPart.isReadOnly());

        assertNotNull(activityPart.getLocation());
        LocationPart locationPart = activityPart.getLocation();
        assertEquals(String.valueOf(location.getId()), locationPart.getId());
        assertEquals(location.getName(), locationPart.getName());
        assertEquals(location.getDescription(), locationPart.getDescription());
        assertEquals(location.getUrl(), locationPart.getUrl());
        assertEquals(location.isReadOnly(), locationPart.isReadOnly());
    }

    @Test
    void of_withNullLocation() {
        // Arrange
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setName("Test Activity");
        activity.setDescription("Test Description");
        activity.setStartTime(Instant.parse("2023-01-01T10:00:00Z"));
        activity.setEndTime(Instant.parse("2023-01-01T12:00:00Z"));
        activity.setLocation(null);
        activity.setReadOnly(false);

        // Act
        ActivityPart activityPart = ActivityPart.of(activity);

        // Assert
        assertEquals(String.valueOf(activity.getId()), activityPart.getId());
        assertEquals(activity.getName(), activityPart.getName());
        assertEquals(activity.getDescription(), activityPart.getDescription());
        assertEquals(activity.getStartTime(), activityPart.getStartTime());
        assertEquals(activity.getEndTime(), activityPart.getEndTime());
        assertFalse(activityPart.isReadOnly());
        assertNull(activityPart.getLocation());
    }

    @Test
    void of_throwsNullPointerException_forNullActivity() {
        assertThrows(NullPointerException.class, () -> {
            ActivityPart.of(null);
        });
    }
}

