package at.control.shiftcontrol.shift.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftRoleServiceTest {
    @Mock
    private ShiftRoleRepository roleRepository;

    @Mock
    private PlanMembershipRepository membershipRepository;

    @Mock
    private PlanInviteRepository inviteRepository;

    @Mock
    private PlanAccess planAccess;

    @Mock
    private Clock clock;

    @InjectMocks
    private ShiftRoleService roles;

    @Test
    void selfAssignableRoleCanBeAssignedByTheMemberButStaysScopedToItsPlan() {
        UUID planId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ShiftRoleEntity role = role(roleId, planId, true);
        PlanMembershipEntity member = new PlanMembershipEntity(planId, userId, false, Instant.parse("2026-05-01T12:00:00Z"));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(userId, false));
        when(membershipRepository.findByPlanIdAndUserId(planId, userId)).thenReturn(Optional.of(member));

        roles.assignToMember(roleId, userId);

        assertEquals(java.util.Set.of(roleId), member.getRoleIds());
        verify(planAccess).requireMember(planId);
    }

    @Test
    void nonSelfAssignableRoleNeedsPlannerAuthority() {
        UUID planId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId, planId, false)));
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(userId, false));
        when(membershipRepository.findByPlanIdAndUserId(planId, userId)).thenReturn(Optional.of(
            new PlanMembershipEntity(planId, userId, false, Instant.parse("2026-05-01T12:00:00Z"))));

        roles.assignToMember(roleId, userId);

        verify(planAccess).requirePlanner(planId);
    }

    @Test
    void createNormalizesRoleDataAndRequiresAPlanner() {
        UUID planId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(roleRepository.existsByPlanIdAndNameIgnoreCase(planId, "Safety")).thenReturn(false);
        when(clock.instant()).thenReturn(Instant.parse("2026-05-01T12:00:00Z"));
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

    @Test
    void roleOperationsRejectAMissingTargetMember() {
        UUID planId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId, planId, true)));
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(userId, false));
        when(membershipRepository.findByPlanIdAndUserId(planId, userId)).thenReturn(Optional.empty());

        assertThrows(at.control.shiftcontrol.shift.error.ShiftNotFoundException.class, () -> roles.assignToMember(roleId, userId));
    }

    @Test
    void deletingARoleRemovesItFromMembersAndOutstandingInvites() {
        UUID planId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        ShiftRoleEntity role = role(roleId, planId, true);
        PlanMembershipEntity membership = new PlanMembershipEntity(planId, UUID.randomUUID(), false,
            Instant.parse("2026-05-01T12:00:00Z"));
        membership.getRoleIds().add(roleId);
        PlanInviteEntity invite = new PlanInviteEntity();
        invite.setPlanId(planId);
        invite.getRoleIds().add(roleId);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(membershipRepository.findAllByPlanId(planId)).thenReturn(java.util.List.of(membership));
        when(inviteRepository.findAllByPlanIdOrderByCreatedAtDesc(planId)).thenReturn(java.util.List.of(invite));

        roles.delete(roleId);

        assertEquals(java.util.Set.of(), membership.getRoleIds());
        assertEquals(java.util.Set.of(), invite.getRoleIds());
        verify(roleRepository).delete(role);
    }

    private static ShiftRoleEntity role(UUID roleId, UUID planId, boolean selfAssignable) {
        ShiftRoleEntity role = new ShiftRoleEntity();
        role.setId(roleId);
        role.setPlanId(planId);
        role.setSelfAssignable(selfAssignable);
        return role;
    }
}
