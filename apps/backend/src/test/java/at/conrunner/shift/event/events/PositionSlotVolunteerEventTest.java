package at.conrunner.shift.event.events;

import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.lib.event.events.PositionSlotVolunteerEvent;
import at.conrunner.shift.lib.event.events.parts.PositionSlotPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class PositionSlotVolunteerEventTest {

    @Test
    void ofInternal() {
        String routingKey = "routingKey";
        PositionSlot positionSlot = mock(PositionSlot.class);
        String volunteerId = "volunteerId";

        PositionSlotPart positionSlotPart = mock(PositionSlotPart.class);
        try (var positionSlotPartMock = org.mockito.Mockito.mockStatic(PositionSlotPart.class)) {
            positionSlotPartMock.when(() -> PositionSlotPart.of(positionSlot)).thenReturn(positionSlotPart);

            PositionSlotVolunteerEvent positionSlotVolunteerEvent = PositionSlotVolunteerEvent.ofInternal(null, routingKey, positionSlot, volunteerId);

            assertEquals(routingKey, positionSlotVolunteerEvent.getRoutingKey());
            assertEquals(positionSlotPart, positionSlotVolunteerEvent.getPositionSlot());
            assertEquals(volunteerId, positionSlotVolunteerEvent.getVolunteerId());
        }
    }
}

