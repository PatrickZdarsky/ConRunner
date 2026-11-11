package at.control.shiftcontrol.shift.event.events.parts;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.lib.event.events.parts.EventPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EventPartTest {

    @Test
    void of() {
        // Arrange
        Event event = new Event();
        event.setId(1L);
        event.setName("Test Event");
        event.setShortDescription("Short Description");
        event.setLongDescription("Long Description");
        event.setStartTime(Instant.parse("2023-01-01T10:00:00Z"));
        event.setEndTime(Instant.parse("2023-01-01T12:00:00Z"));

        // Act
        EventPart eventPart = EventPart.of(event);

        // Assert
        assertEquals(String.valueOf(event.getId()), eventPart.getId());
        assertEquals(event.getName(), eventPart.getName());
        assertEquals(event.getShortDescription(), eventPart.getShortDescription());
        assertEquals(event.getLongDescription(), eventPart.getLongDescription());
        assertEquals(event.getStartTime(), eventPart.getStartTime());
        assertEquals(event.getEndTime(), eventPart.getEndTime());
    }

    @Test
    void of_throwsNullPointerException_forNullEvent() {
        assertThrows(NullPointerException.class, () -> {
            EventPart.of(null);
        });
    }
}

