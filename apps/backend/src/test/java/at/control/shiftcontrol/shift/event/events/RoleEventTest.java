package at.control.shiftcontrol.shift.event.events;

import at.control.shiftcontrol.shift.entity.Role;
import at.control.shiftcontrol.shift.lib.event.events.RoleEvent;
import at.control.shiftcontrol.shift.lib.event.events.parts.RolePart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class RoleEventTest {

    @Test
    void ofInternal() {
        Role role = mock(Role.class);
        String routingKey = "routingKey";

        RolePart rolePart = mock(RolePart.class);
        try (var rolePartMock = org.mockito.Mockito.mockStatic(RolePart.class)) {
            rolePartMock.when(() -> RolePart.of(role)).thenReturn(rolePart);

            RoleEvent roleEvent = RoleEvent.ofInternal(null, routingKey, role);

            assertEquals(rolePart, roleEvent.getRole());
            assertEquals(routingKey, roleEvent.getRoutingKey());
        }
    }
}

