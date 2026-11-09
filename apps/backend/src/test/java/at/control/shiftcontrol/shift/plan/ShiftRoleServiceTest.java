package at.control.shiftcontrol.shift.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftRoleServiceTest {
    @Mock private ShiftRoleRepository roleRepository;
    @Mock private ShiftPlanRepository planRepository;
    @Mock private PlanMembershipRepository membershipRepository;
    @Mock private PlanInviteRepository inviteRepository;
    @Mock private PlanAccess planAccess;
    @Mock private Clock clock;
    @InjectMocks private ShiftRoleService roles;

    @Test
    void selfAssignableRoleCanBeAssignedByTheMemberButStaysScopedToItsPlan() {
        UUID planId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ShiftRoleEntity role = role(roleId, planId, true);
        PlanMembershipEntity member = new PlanMembershipEntity(planId, userId, false, Instant.parse("2026-09-05T12:00:00Z"));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(userId, false));
        when(membershipRepository.findByPlanIdAndUserId(planId, userId)).thenReturn(Optional.of(member));

        roles.assignToMember(roleId, userId);

        assertEquals(java.util.Set.of(roleId), member.getRoleIds());
        verify(planAccess).requireMember(planId);
        verify(membershipRepository).save(member);
    }

    @Test
    void createNormalizesRoleDataAndRequiresAPlanner() {
        UUID planId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(planRepository.existsById(planId)).thenReturn(true);
        when(roleRepository.existsByPlanIdAndNameIgnoreCase(planId, "Safety")).thenReturn(false);
        when(clock.instant()).thenReturn(Instant.parse("2026-09-05T12:00:00Z"));
        when(roleRepository.save(any(ShiftRoleEntity.class))).thenAnswer(invocation -> {
            ShiftRoleEntity role = invocation.getArgument(0);
            role.setId(roleId);
            return role;
        });

        ShiftRoleDetails result = roles.create(planId, new ShiftRoleData(" Safety ", "  ", true));

        assertEquals(roleId, result.id());
        assertEquals("Safety", result.name());
        assertEquals(null, result.description());
        verify(planAccess).requirePlanner(planId);
    }

    private static ShiftRoleEntity role(UUID roleId, UUID planId, boolean selfAssignable) {
        ShiftRoleEntity role = new ShiftRoleEntity();
        role.setId(roleId);
        role.setPlanId(planId);
        role.setSelfAssignable(selfAssignable);
        return role;
    }
}
