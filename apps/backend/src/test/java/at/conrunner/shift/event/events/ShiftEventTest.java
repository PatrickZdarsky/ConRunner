package at.conrunner.shift.event.events;

import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.lib.event.events.ShiftEvent;
import at.conrunner.shift.lib.event.events.parts.ShiftPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class ShiftEventTest {

    @Test
    void ofInternal() {
        Shift shift = mock(Shift.class);
        String routingKey = "routingKey";

        ShiftPart shiftPart = mock(ShiftPart.class);
        try (var shiftPartMock = org.mockito.Mockito.mockStatic(ShiftPart.class)) {
            shiftPartMock.when(() -> ShiftPart.of(shift)).thenReturn(shiftPart);

            ShiftEvent shiftEvent = ShiftEvent.ofInternal(null, routingKey, shift);

            assertEquals(shiftPart, shiftEvent.getShift());
            assertEquals(routingKey, shiftEvent.getRoutingKey());
        }
    }
}

