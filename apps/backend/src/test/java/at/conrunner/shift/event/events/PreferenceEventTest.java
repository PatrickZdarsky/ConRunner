package at.conrunner.shift.event.events;

import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.lib.event.events.PreferenceEvent;
import at.conrunner.shift.lib.event.events.parts.PositionSlotPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class PreferenceEventTest {

    @Test
    void ofInternal() {
        String routingKey = "routingKey";
        String volunteerId = "volunteerId";
        int preferenceLevel = 1;
        PositionSlot positionSlot = mock(PositionSlot.class);

        PositionSlotPart positionSlotPart = mock(PositionSlotPart.class);
        try (var positionSlotPartMock = org.mockito.Mockito.mockStatic(PositionSlotPart.class)) {
            positionSlotPartMock.when(() -> PositionSlotPart.of(positionSlot)).thenReturn(positionSlotPart);

            PreferenceEvent preferenceEvent = PreferenceEvent.ofInternal(null, routingKey, volunteerId, preferenceLevel, positionSlot);

            assertEquals(routingKey, preferenceEvent.getRoutingKey());
            assertEquals(volunteerId, preferenceEvent.getVolunteerId());
            assertEquals(preferenceLevel, preferenceEvent.getPreferenceLevel());
            assertEquals(positionSlotPart, preferenceEvent.getPositionSlot());
        }
    }
}

