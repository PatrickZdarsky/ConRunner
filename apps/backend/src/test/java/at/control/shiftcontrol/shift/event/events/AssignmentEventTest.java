package at.control.shiftcontrol.shift.event.events;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.lib.event.events.AssignmentEvent;
import at.control.shiftcontrol.shift.lib.event.events.parts.AssignmentPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class AssignmentEventTest {

    @Test
    void of() {
        Assignment assignment = mock(Assignment.class);
        String routingKey = "routingKey";

        AssignmentPart assignmentPart = mock(AssignmentPart.class);
        try (var assignmentPartMock = org.mockito.Mockito.mockStatic(AssignmentPart.class)) {
            assignmentPartMock.when(() -> AssignmentPart.of(assignment)).thenReturn(assignmentPart);

            AssignmentEvent assignmentEvent = AssignmentEvent.of(null, routingKey, assignment);

            assertEquals(assignmentPart, assignmentEvent.getAssignment());
            assertEquals(routingKey, assignmentEvent.getRoutingKey());
        }
    }
}

