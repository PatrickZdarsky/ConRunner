package at.control.shiftcontrol.shift.event.events;

import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.ShiftPlanInvite;
import at.control.shiftcontrol.shift.lib.event.events.ShiftPlanInviteEvent;
import at.control.shiftcontrol.shift.lib.event.events.parts.InvitePart;
import at.control.shiftcontrol.shift.lib.event.events.parts.ShiftPlanPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShiftPlanInviteEventTest {

    @Test
    void ofInternal() {
        String routingKey = "routingKey";
        ShiftPlanInvite invite = mock(ShiftPlanInvite.class);
        ShiftPlan shiftPlan = mock(ShiftPlan.class);
        when(invite.getShiftPlan()).thenReturn(shiftPlan);


        ShiftPlanPart shiftPlanPart = mock(ShiftPlanPart.class);
        InvitePart invitePart = mock(InvitePart.class);
        try (var shiftPlanPartMock = org.mockito.Mockito.mockStatic(ShiftPlanPart.class);
             var invitePartMock = org.mockito.Mockito.mockStatic(InvitePart.class)) {
            shiftPlanPartMock.when(() -> ShiftPlanPart.of(shiftPlan)).thenReturn(shiftPlanPart);
            invitePartMock.when(() -> InvitePart.of(invite)).thenReturn(invitePart);

            ShiftPlanInviteEvent shiftPlanInviteEvent = ShiftPlanInviteEvent.ofInternal(null, routingKey, invite);

            assertEquals(routingKey, shiftPlanInviteEvent.getRoutingKey());
            assertEquals(shiftPlanPart, shiftPlanInviteEvent.getShiftPlan());
            assertEquals(invitePart, shiftPlanInviteEvent.getInvite());
        }
    }
}

