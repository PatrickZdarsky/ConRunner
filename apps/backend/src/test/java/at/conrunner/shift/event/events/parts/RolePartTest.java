package at.conrunner.shift.event.events.parts;

import at.conrunner.shift.entity.Role;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.lib.event.events.parts.RolePart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RolePartTest {

    @Test
    void of() {
        // Arrange
        ShiftPlan shiftPlan = new ShiftPlan();
        shiftPlan.setId(1L);

        Role role = new Role();
        role.setId(1L);
        role.setShiftPlan(shiftPlan);
        role.setName("Test Role");
        role.setDescription("Test Description");
        role.setSelfAssignable(true);

        // Act
        RolePart rolePart = RolePart.of(role);

        // Assert
        assertEquals(String.valueOf(role.getId()), rolePart.getId());
        assertEquals(String.valueOf(role.getShiftPlan().getId()), rolePart.getShiftPlanId());
        assertEquals(role.getName(), rolePart.getName());
        assertEquals(role.getDescription(), rolePart.getDescription());
        assertEquals(role.isSelfAssignable(), rolePart.isSelfAssignable());
    }

    @Test
    void of_throwsNullPointerException_forNullRole() {
        assertThrows(NullPointerException.class, () -> {
            RolePart.of((Role) null);
        });
    }
}

