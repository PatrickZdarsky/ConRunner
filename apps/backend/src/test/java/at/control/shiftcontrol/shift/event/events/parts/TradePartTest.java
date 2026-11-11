package at.control.shiftcontrol.shift.event.events.parts;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentKey;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.event.events.parts.AssignmentPart;
import at.control.shiftcontrol.shift.lib.event.events.parts.TradePart;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.type.TradeStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradePartTest {

    @Test
    void of() {
        // Arrange
        Volunteer volunteer1 = new Volunteer();
        volunteer1.setId(java.util.UUID.randomUUID());
        Volunteer volunteer2 = new Volunteer();
        volunteer2.setId(java.util.UUID.randomUUID());

        Shift shift = new Shift();
        ShiftPlan shiftPlan = new ShiftPlan();
        shift.setShiftPlan(shiftPlan);
        Event event = new Event();
        shiftPlan.setEvent(event);

        PositionSlot offeringSlot = new PositionSlot();
        offeringSlot.setId(1L);
        offeringSlot.setName("Offering Slot");
        offeringSlot.setDescription("Offering Description");
        offeringSlot.setShift(shift);

        AssignmentKey offeringId = new AssignmentKey(1L, "volunteer-1");
        Assignment offeringAssignment = new Assignment();
        offeringAssignment.setStatus(AssignmentStatus.ACCEPTED);
        offeringAssignment.setPositionSlot(offeringSlot);
        offeringAssignment.setAssignedVolunteer(volunteer1);

        PositionSlot requestedSlot = new PositionSlot();
        requestedSlot.setId(2L);
        requestedSlot.setName("Requested Slot");
        requestedSlot.setDescription("Requested Description");
        requestedSlot.setShift(shift);

        AssignmentKey requestedId = new AssignmentKey(2L, "volunteer-2");
        Assignment requestedAssignment = new Assignment();
        requestedAssignment.setStatus(AssignmentStatus.ACCEPTED);
        requestedAssignment.setPositionSlot(requestedSlot);
        requestedAssignment.setAssignedVolunteer(volunteer2);

        AssignmentSwitchRequest tradeRequest = new AssignmentSwitchRequest();
        tradeRequest.setOfferingAssignment(offeringAssignment);
        tradeRequest.setRequestedAssignment(requestedAssignment);
        tradeRequest.setStatus(TradeStatus.OPEN);
        tradeRequest.setCreatedAt(Instant.parse("2023-01-01T10:00:00Z"));

        // Act
        TradePart tradePart = TradePart.of(tradeRequest);

        // Assert
        assertNotNull(tradePart);
        assertEquals(TradeStatus.OPEN, tradePart.getStatus());
        assertEquals(tradeRequest.getCreatedAt(), tradePart.getCreatedAt());

        assertNotNull(tradePart.getOfferingAssignment());
        AssignmentPart offeringPart = tradePart.getOfferingAssignment();
        assertEquals(volunteer1.getId().toString(), offeringPart.getVolunteerId());
        assertEquals(AssignmentStatus.ACCEPTED, offeringPart.getStatus());
        assertEquals(1L, offeringPart.getPositionSlot().getPositionSlotId());

        assertNotNull(tradePart.getRequestedAssignment());
        AssignmentPart requestedPart = tradePart.getRequestedAssignment();
        assertEquals(volunteer2.getId().toString(), requestedPart.getVolunteerId());
        assertEquals(AssignmentStatus.ACCEPTED, requestedPart.getStatus());
        assertEquals(2L, requestedPart.getPositionSlot().getPositionSlotId());
    }

    @Test
    void of_throwsNullPointerException_forNullTradeRequest() {
        assertThrows(NullPointerException.class, () -> {
            TradePart.of(null);
        });
    }
}

