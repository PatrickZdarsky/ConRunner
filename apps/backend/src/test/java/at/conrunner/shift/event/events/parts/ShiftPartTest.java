package at.conrunner.shift.event.events.parts;

import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.lib.event.events.parts.PositionSlotPart;
import at.conrunner.shift.lib.event.events.parts.ShiftPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShiftPartTest {

    @Test
    void of_withRelations() {
        // Arrange
        Activity activity = new Activity();
        activity.setId(1L);

        Location location = new Location();
        location.setId(1L);

        Shift shift = new Shift();
        ShiftPlan shiftPlan = new ShiftPlan();
        shift.setShiftPlan(shiftPlan);
        Event event = new Event();
        shiftPlan.setEvent(event);

        PositionSlot positionSlot = new PositionSlot();
        positionSlot.setId(1L);
        positionSlot.setName("Test Slot");
        positionSlot.setDescription("Test Description");
        positionSlot.setShift(shift);

        shift.setId(1L);
        shift.setName("Test Shift");
        shift.setShortDescription("Short Description");
        shift.setLongDescription("Long Description");
        shift.setStartTime(Instant.parse("2023-01-01T10:00:00Z"));
        shift.setEndTime(Instant.parse("2023-01-01T12:00:00Z"));
        shift.setRelatedActivity(activity);
        shift.setLocation(location);
        shift.setSlots(Collections.singletonList(positionSlot));

        // Act
        ShiftPart shiftPart = ShiftPart.of(shift);

        // Assert
        assertEquals(String.valueOf(shift.getId()), shiftPart.getId());
        assertEquals(shift.getName(), shiftPart.getName());
        assertEquals(shift.getShortDescription(), shiftPart.getShortDescription());
        assertEquals(shift.getLongDescription(), shiftPart.getLongDescription());
        assertEquals(shift.getStartTime(), shiftPart.getStartTime());
        assertEquals(shift.getEndTime(), shiftPart.getEndTime());
        assertEquals(activity.getId(), shiftPart.getRelatedActivityId());
        assertEquals(location.getId(), shiftPart.getLocationId());

        assertNotNull(shiftPart.getPositionSlots());
        assertEquals(1, shiftPart.getPositionSlots().size());
        PositionSlotPart slotPart = shiftPart.getPositionSlots().iterator().next();
        assertEquals(positionSlot.getId(), slotPart.getPositionSlotId());
        assertEquals(positionSlot.getName(), slotPart.getPositionSlotName());
        assertEquals(positionSlot.getDescription(), slotPart.getPositionSlotDescription());
    }

    @Test
    void of_withNullRelations() {
        // Arrange
        Shift shift = new Shift();
        shift.setId(1L);
        shift.setName("Test Shift");
        shift.setStartTime(Instant.now());
        shift.setEndTime(Instant.now());
        shift.setRelatedActivity(null);
        shift.setLocation(null);
        shift.setSlots(Collections.emptyList());

        // Act
        ShiftPart shiftPart = ShiftPart.of(shift);

        // Assert
        assertEquals(String.valueOf(shift.getId()), shiftPart.getId());
        assertEquals(null, shiftPart.getRelatedActivityId());
        assertEquals(null, shiftPart.getLocationId());
        assertTrue(shiftPart.getPositionSlots().isEmpty());
    }

    @Test
    void of_throwsNullPointerException_forNullShift() {
        assertThrows(NullPointerException.class, () -> {
            ShiftPart.of(null);
        });
    }
}

