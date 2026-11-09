package at.control.shiftcontrol.shift.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanInviteServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    @Mock private PlanInviteRepository inviteRepository;
    @Mock private ShiftRoleRepository roleRepository;
    @Mock private ShiftPlanRepository planRepository;
    @Mock private PlanMembershipRepository membershipRepository;
    @Mock private VolunteerProfileService volunteerProfileService;
    @Mock private PlanAccess planAccess;
    @Mock private Clock clock;
    @InjectMocks private PlanInviteService invites;

    @Test
    void createsARedisplayableHighEntropyInviteCode() {
        UUID planId = UUID.randomUUID();
        UUID inviteId = UUID.randomUUID();
        when(clock.instant()).thenReturn(NOW);
        when(planRepository.existsById(planId)).thenReturn(true);
        when(inviteRepository.save(any(PlanInviteEntity.class))).thenAnswer(invocation -> {
            PlanInviteEntity invite = invocation.getArgument(0);
            invite.setId(inviteId);
            return invite;
        });

        IssuedPlanInvite issued = invites.create(planId,
            new PlanInviteCreateData(PlanInviteType.VOLUNTEER, NOW.plusSeconds(3600), 3, Set.of()));

        assertEquals(inviteId, issued.inviteId());
        assertTrue(issued.code().length() >= 43);
        ArgumentCaptor<PlanInviteEntity> stored = ArgumentCaptor.forClass(PlanInviteEntity.class);
        verify(inviteRepository).save(stored.capture());
        assertEquals(issued.code(), stored.getValue().getCode());
    }

    @Test
    void redeemCreatesOneMembershipAndConsumesTheLastAllowedUse() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        PlanInviteEntity invite = invite(planId, PlanInviteType.VOLUNTEER, 1, 0, true, Set.of(roleId));
        when(clock.instant()).thenReturn(NOW);
        when(inviteRepository.findByCodeForUpdate("valid-code")).thenReturn(Optional.of(invite));
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(userId, false));
        when(membershipRepository.findByPlanIdAndUserId(planId, userId)).thenReturn(Optional.empty());
        when(membershipRepository.save(any(PlanMembershipEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invites.redeem(" valid-code ");

        ArgumentCaptor<PlanMembershipEntity> membership = ArgumentCaptor.forClass(PlanMembershipEntity.class);
        verify(membershipRepository, org.mockito.Mockito.atLeastOnce()).save(membership.capture());
        assertEquals(userId, membership.getValue().getUserId());
        assertFalse(membership.getValue().isPlanner());
        assertEquals(Set.of(roleId), membership.getValue().getRoleIds());
        assertEquals(1, invite.getUses());
        assertFalse(invite.isActive());
        verify(volunteerProfileService).ensureProfile(userId);
    }

    @Test
    void redeemIsIdempotentAndCanUpgradeAnExistingVolunteerToPlanner() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PlanInviteEntity invite = invite(planId, PlanInviteType.PLANNER, null, 4, true, Set.of());
        PlanMembershipEntity membership = new PlanMembershipEntity(planId, userId, false, NOW.minusSeconds(60));
        when(clock.instant()).thenReturn(NOW);
        when(inviteRepository.findByCodeForUpdate("planner-code")).thenReturn(Optional.of(invite));
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(userId, false));
        when(membershipRepository.findByPlanIdAndUserId(planId, userId)).thenReturn(Optional.of(membership));

        invites.redeem("planner-code");

        assertTrue(membership.isPlanner());
        assertEquals(4, invite.getUses());
    }

    private static PlanInviteEntity invite(UUID planId, PlanInviteType type, Integer useLimit, int uses, boolean active,
                                            Set<UUID> roleIds) {
        PlanInviteEntity invite = new PlanInviteEntity();
        invite.setPlanId(planId);
        invite.setCode("code-" + UUID.randomUUID());
        invite.setType(type);
        invite.setUseLimit(useLimit);
        invite.setUses(uses);
        invite.setActive(active);
        invite.setRoleIds(roleIds);
        return invite;
    }
}
