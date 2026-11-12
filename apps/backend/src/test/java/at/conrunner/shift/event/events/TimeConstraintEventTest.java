package at.conrunner.shift.event.events;

import at.conrunner.shift.entity.TimeConstraint;
import at.conrunner.shift.lib.event.events.TimeConstraintEvent;
import at.conrunner.shift.lib.event.events.parts.TimeConstraintPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class TimeConstraintEventTest {

    @Test
    void ofInternal() {
        TimeConstraint timeConstraint = mock(TimeConstraint.class);
        String routingKey = "routingKey";

        TimeConstraintPart timeConstraintPart = mock(TimeConstraintPart.class);
        try (var timeConstraintPartMock = org.mockito.Mockito.mockStatic(TimeConstraintPart.class)) {
            timeConstraintPartMock.when(() -> TimeConstraintPart.of(timeConstraint)).thenReturn(timeConstraintPart);

            TimeConstraintEvent timeConstraintEvent = TimeConstraintEvent.ofInternal(null, routingKey, timeConstraint);

            assertEquals(routingKey, timeConstraintEvent.getRoutingKey());
            assertEquals(timeConstraintPart, timeConstraintEvent.getTimeConstraint());
        }
    }
}

