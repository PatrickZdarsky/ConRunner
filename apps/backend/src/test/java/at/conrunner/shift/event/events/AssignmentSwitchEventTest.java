package at.conrunner.shift.event.events;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.event.events.AssignmentSwitchEvent;
import at.conrunner.shift.lib.event.events.parts.AssignmentPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignmentSwitchEventTest {

    @Test
    void assignmentSwitched() {
        Volunteer requestedVolunteer = mock(Volunteer.class);
        Volunteer offeringVolunteer = mock(Volunteer.class);
        Assignment requestedAssignment = mock(Assignment.class);
        Assignment offeringAssignment = mock(Assignment.class);
        when(requestedAssignment.getAssignedVolunteer()).thenReturn(requestedVolunteer);
        when(offeringAssignment.getAssignedVolunteer()).thenReturn(offeringVolunteer);
        when(requestedVolunteer.getId()).thenReturn(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(offeringVolunteer.getId()).thenReturn(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"));

        AssignmentPart requestedAssignmentPart = mock(AssignmentPart.class);
        AssignmentPart offeringAssignmentPart = mock(AssignmentPart.class);
        when(requestedAssignmentPart.getVolunteerId()).thenReturn("1");
        when(offeringAssignmentPart.getVolunteerId()).thenReturn("2");

        try (var assignmentPartMock = org.mockito.Mockito.mockStatic(AssignmentPart.class)) {
            assignmentPartMock.when(() -> AssignmentPart.of(requestedAssignment)).thenReturn(requestedAssignmentPart);
            assignmentPartMock.when(() -> AssignmentPart.of(offeringAssignment)).thenReturn(offeringAssignmentPart);

            AssignmentSwitchEvent assignmentSwitchEvent = AssignmentSwitchEvent.assignmentSwitched(requestedAssignment, offeringAssignment);

            assertEquals(requestedAssignmentPart, assignmentSwitchEvent.getRequestedAssignment());
            assertEquals(offeringAssignmentPart, assignmentSwitchEvent.getOfferingAssignment());
        }
    }

    @Test
    void getRoutingKey() {
        AssignmentPart requestedAssignmentPart = mock(AssignmentPart.class);
        AssignmentPart offeringAssignmentPart = mock(AssignmentPart.class);
        when(requestedAssignmentPart.getVolunteerId()).thenReturn("1");
        when(offeringAssignmentPart.getVolunteerId()).thenReturn("2");
        AssignmentSwitchEvent assignmentSwitchEvent = new AssignmentSwitchEvent(requestedAssignmentPart, offeringAssignmentPart);

        assertEquals("trade.request.completed.1.2", assignmentSwitchEvent.getRoutingKey());
    }
}

