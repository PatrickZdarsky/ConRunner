package at.control.shiftcontrol.shift.event.events.parts;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.lib.event.events.parts.PositionSlotPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionSlotPartTest {

    @Test
    void of() {
        // Arrange
        Shift shift = new Shift();
        ShiftPlan shiftPlan = new ShiftPlan();
        shift.setShiftPlan(shiftPlan);
        Event event = new Event();
        shiftPlan.setEvent(event);
        PositionSlot positionSlot = new PositionSlot();
        positionSlot.setId(1L);
        positionSlot.setName("Test Position Slot");
        positionSlot.setDescription("Test Description");
        positionSlot.setShift(shift);

        // Act
        PositionSlotPart positionSlotPart = PositionSlotPart.of(positionSlot);

        // Assert
        assertEquals(positionSlot.getId(), positionSlotPart.getPositionSlotId());
        assertEquals(positionSlot.getName(), positionSlotPart.getPositionSlotName());
        assertEquals(positionSlot.getDescription(), positionSlotPart.getPositionSlotDescription());
    }

    @Test
    void of_throwsNullPointerException_forNullPositionSlot() {
        assertThrows(NullPointerException.class, () -> {
            PositionSlotPart.of(null);
        });
    }
}

