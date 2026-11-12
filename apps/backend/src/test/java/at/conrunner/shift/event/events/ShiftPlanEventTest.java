package at.conrunner.shift.event.events;

import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.lib.event.events.ShiftPlanEvent;
import at.conrunner.shift.lib.event.events.parts.ShiftPlanPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class ShiftPlanEventTest {

    @Test
    void ofInternal() {
        ShiftPlan shiftPlan = mock(ShiftPlan.class);
        String routingKey = "routingKey";

        ShiftPlanPart shiftPlanPart = mock(ShiftPlanPart.class);
        try (var shiftPlanPartMock = org.mockito.Mockito.mockStatic(ShiftPlanPart.class)) {
            shiftPlanPartMock.when(() -> ShiftPlanPart.of(shiftPlan)).thenReturn(shiftPlanPart);

            ShiftPlanEvent shiftPlanEvent = ShiftPlanEvent.ofInternal(null, routingKey, shiftPlan);

            assertEquals(routingKey, shiftPlanEvent.getRoutingKey());
            assertEquals(shiftPlanPart, shiftPlanEvent.getShiftPlan());
        }
    }
}

