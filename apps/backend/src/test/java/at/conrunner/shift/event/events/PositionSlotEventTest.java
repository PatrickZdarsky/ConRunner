package at.conrunner.shift.event.events;

import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.lib.event.events.PositionSlotEvent;
import at.conrunner.shift.lib.event.events.parts.PositionSlotPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class PositionSlotEventTest {

    @Test
    void ofInternal() {
        PositionSlot positionSlot = mock(PositionSlot.class);
        String routingKey = "routingKey";

        PositionSlotPart positionSlotPart = mock(PositionSlotPart.class);
        try (var positionSlotPartMock = org.mockito.Mockito.mockStatic(PositionSlotPart.class)) {
            positionSlotPartMock.when(() -> PositionSlotPart.of(positionSlot)).thenReturn(positionSlotPart);

            PositionSlotEvent positionSlotEvent = PositionSlotEvent.ofInternal(null, routingKey, positionSlot);

            assertEquals(routingKey, positionSlotEvent.getRoutingKey());
            assertEquals(positionSlotPart, positionSlotEvent.getPositionSlot());
        }
    }
}

