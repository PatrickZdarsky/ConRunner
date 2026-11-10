package at.control.shiftcontrol.shift.event.events;

import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.lib.event.events.ShiftPlanVolunteerEvent;
import at.control.shiftcontrol.shift.lib.event.events.parts.ShiftPlanPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class ShiftPlanVolunteerEventTest {

    @Test
    void ofInternal() {
        String routingKey = "routingKey";
        ShiftPlan shiftPlan = mock(ShiftPlan.class);
        String volunteerId = "volunteerId";

        ShiftPlanPart shiftPlanPart = mock(ShiftPlanPart.class);
        try (var shiftPlanPartMock = org.mockito.Mockito.mockStatic(ShiftPlanPart.class)) {
            shiftPlanPartMock.when(() -> ShiftPlanPart.of(shiftPlan)).thenReturn(shiftPlanPart);

            ShiftPlanVolunteerEvent shiftPlanVolunteerEvent = ShiftPlanVolunteerEvent.ofInternal(null, routingKey, shiftPlan, volunteerId);

            assertEquals(routingKey, shiftPlanVolunteerEvent.getRoutingKey());
            assertEquals(shiftPlanPart, shiftPlanVolunteerEvent.getShiftPlan());
            assertEquals(volunteerId, shiftPlanVolunteerEvent.getVolunteerId());
        }
    }
}

