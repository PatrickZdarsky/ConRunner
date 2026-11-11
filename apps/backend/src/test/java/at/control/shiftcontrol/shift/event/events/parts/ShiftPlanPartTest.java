package at.control.shiftcontrol.shift.event.events.parts;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.lib.event.events.parts.ShiftPlanPart;
import at.control.shiftcontrol.shift.lib.type.LockStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ShiftPlanPartTest {

    @Test
    void of() {
        // Arrange
        Shift shift = new Shift();
        ShiftPlan shiftPlan = new ShiftPlan();
        shift.setShiftPlan(shiftPlan);
        Event event = new Event();
        shiftPlan.setEvent(event);
        shiftPlan.setId(1L);
        shiftPlan.setName("Test Shift Plan");
        shiftPlan.setShortDescription("Short Description");
        shiftPlan.setLongDescription("Long Description");
        shiftPlan.setLockStatus(LockStatus.LOCKED);

        // Act
        ShiftPlanPart shiftPlanPart = ShiftPlanPart.of(shiftPlan);

        // Assert
        assertEquals(String.valueOf(shiftPlan.getId()), shiftPlanPart.getId());
        assertEquals(shiftPlan.getName(), shiftPlanPart.getName());
        assertEquals(shiftPlan.getShortDescription(), shiftPlanPart.getShortDescription());
        assertEquals(shiftPlan.getLongDescription(), shiftPlanPart.getLongDescription());
        assertEquals(shiftPlan.getLockStatus(), shiftPlanPart.getLockStatus());
    }

    @Test
    void of_throwsNullPointerException_forNullShiftPlan() {
        assertThrows(NullPointerException.class, () -> {
            ShiftPlanPart.of((ShiftPlan) null);
        });
    }
}

