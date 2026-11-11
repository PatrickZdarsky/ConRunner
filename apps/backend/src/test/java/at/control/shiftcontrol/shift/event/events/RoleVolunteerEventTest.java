package at.control.shiftcontrol.shift.event.events;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.Role;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.lib.event.events.RoleVolunteerEvent;
import at.control.shiftcontrol.shift.lib.event.events.parts.RolePart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class RoleVolunteerEventTest {

    @Test
    void ofInternal() {
        String routingKey = "routingKey";
        Role role = new Role();
        ShiftPlan shiftPlan = new ShiftPlan();
        role.setShiftPlan(shiftPlan);
        Event event = new Event();
        shiftPlan.setEvent(event);
        String volunteerId = "volunteerId";

        RolePart rolePart = mock(RolePart.class);
        try (var rolePartMock = org.mockito.Mockito.mockStatic(RolePart.class)) {
            rolePartMock.when(() -> RolePart.of(role)).thenReturn(rolePart);

            RoleVolunteerEvent roleVolunteerEvent = RoleVolunteerEvent.ofInternal(null, routingKey, role, volunteerId);

            assertEquals(routingKey, roleVolunteerEvent.getRoutingKey());
            assertEquals(rolePart, roleVolunteerEvent.getRole());
            assertEquals(volunteerId, roleVolunteerEvent.getVolunteerId());
        }
    }
}

