package at.conrunner.shift.event.events.parts;

import at.conrunner.shift.entity.Role;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.ShiftPlanInvite;
import at.conrunner.shift.lib.event.events.parts.InvitePart;
import at.conrunner.shift.lib.event.events.parts.RolePart;
import at.conrunner.shift.lib.type.ShiftPlanInviteType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvitePartTest {

    @Test
    void of_withRoles() {
        // Arrange
        ShiftPlan shiftPlan = new ShiftPlan();
        shiftPlan.setId(1L);

        Role role = new Role();
        role.setId(1L);
        role.setShiftPlan(shiftPlan);
        role.setName("Test Role");
        role.setDescription("Test Description");
        role.setSelfAssignable(true);

        ShiftPlanInvite invite = new ShiftPlanInvite();
        invite.setId(1L);
        invite.setCode("test-code");
        invite.setType(ShiftPlanInviteType.VOLUNTEER_JOIN);
        invite.setActive(true);
        invite.setExpiresAt(Instant.parse("2023-01-01T10:00:00Z"));
        invite.setMaxUses(10);
        invite.setUses(5);
        invite.setAutoAssignRoles(Collections.singletonList(role));
        invite.setCreatedAt(Instant.parse("2023-01-01T09:00:00Z"));
        invite.setRevokedAt(null);

        // Act
        InvitePart invitePart = InvitePart.of(invite);

        // Assert
        assertEquals(String.valueOf(invite.getId()), invitePart.getId());
        assertEquals(invite.getCode(), invitePart.getCode());
        assertEquals(invite.getType(), invitePart.getType());
        assertEquals(invite.isActive(), invitePart.isActive());
        assertEquals(invite.getExpiresAt(), invitePart.getExpiresAt());
        assertEquals(invite.getMaxUses(), invitePart.getMaxUses());
        assertEquals(invite.getUses(), invitePart.getUsedCount());
        assertEquals(invite.getCreatedAt(), invitePart.getCreatedAt());
        assertNull(invitePart.getRevokedAt());

        assertNotNull(invitePart.getAutoAssignedRoles());
        assertEquals(1, invitePart.getAutoAssignedRoles().size());
        RolePart rolePart = invitePart.getAutoAssignedRoles().iterator().next();
        assertEquals(String.valueOf(role.getId()), rolePart.getId());
        assertEquals(String.valueOf(role.getShiftPlan().getId()), rolePart.getShiftPlanId());
        assertEquals(role.getName(), rolePart.getName());
        assertEquals(role.getDescription(), rolePart.getDescription());
        assertEquals(role.isSelfAssignable(), rolePart.isSelfAssignable());
    }

    @Test
    void of_withNullRoles() {
        // Arrange
        ShiftPlanInvite invite = new ShiftPlanInvite();
        invite.setId(1L);
        invite.setCode("test-code");
        invite.setType(ShiftPlanInviteType.VOLUNTEER_JOIN);
        invite.setActive(true);
        invite.setExpiresAt(null);
        invite.setMaxUses(null);
        invite.setUses(0);
        invite.setAutoAssignRoles(null);
        invite.setCreatedAt(Instant.now());
        invite.setRevokedAt(Instant.now());

        // Act
        InvitePart invitePart = InvitePart.of(invite);

        // Assert
        assertEquals(String.valueOf(invite.getId()), invitePart.getId());
        assertNull(invitePart.getAutoAssignedRoles());
        assertNotNull(invitePart.getRevokedAt());
    }

    @Test
    void of_throwsNullPointerException_forNullInvite() {
        assertThrows(NullPointerException.class, () -> {
            InvitePart.of(null);
        });
    }
}

