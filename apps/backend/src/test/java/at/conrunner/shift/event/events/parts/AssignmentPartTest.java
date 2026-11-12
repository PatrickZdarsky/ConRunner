package at.conrunner.shift.event.events.parts;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.event.events.parts.AssignmentPart;
import at.conrunner.shift.lib.event.events.parts.PositionSlotPart;
import at.conrunner.shift.lib.type.AssignmentStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssignmentPartTest {

    @Test
    void of() {
        // Arrange
        Shift shift = new Shift();
        ShiftPlan shiftPlan = new ShiftPlan();
        shift.setShiftPlan(shiftPlan);
        Event event = new Event();
        shiftPlan.setEvent(event);
        PositionSlot positionSlot = new PositionSlot();
        positionSlot.setId(1L);
        positionSlot.setName("Test Position Slot");
        positionSlot.setDescription("Test Description");
        positionSlot.setShift(shift);
        Volunteer volunteer = new Volunteer();
        volunteer.setId(java.util.UUID.randomUUID());

        Assignment assignment = new Assignment();
        assignment.setStatus(AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
        assignment.setPositionSlot(positionSlot);
        assignment.setAssignedVolunteer(volunteer);

        // Act
        AssignmentPart assignmentPart = AssignmentPart.of(assignment);

        // Assert
        assertNotNull(assignmentPart);
        assertEquals(volunteer.getId().toString(), assignmentPart.getVolunteerId());
        assertEquals(AssignmentStatus.REQUEST_FOR_ASSIGNMENT, assignmentPart.getStatus());

        assertNotNull(assignmentPart.getPositionSlot());
        PositionSlotPart positionSlotPart = assignmentPart.getPositionSlot();
        assertEquals(positionSlot.getId(), positionSlotPart.getPositionSlotId());
        assertEquals(positionSlot.getName(), positionSlotPart.getPositionSlotName());
        assertEquals(positionSlot.getDescription(), positionSlotPart.getPositionSlotDescription());
    }

    @Test
    void of_throwsNullPointerException_forNullAssignment() {
        assertThrows(NullPointerException.class, () -> {
            AssignmentPart.of((Assignment) null);
        });
    }
}

