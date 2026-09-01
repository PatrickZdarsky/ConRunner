package at.control.shiftcontrol.shift.plan;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftAccessDeniedException;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlanInviteService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int INVITE_BYTES = 32;

    private final PlanInviteRepository inviteRepository;
    private final ShiftRoleRepository roleRepository;
    private final PlanMembershipRepository membershipRepository;
    private final ShiftPlanService shiftPlanService;
    private final VolunteerProfileService volunteerProfileService;
    private final PlanAccess planAccess;
    private final Clock clock;

    public Collection<PlanInviteDetails> list(UUID planId) {
        planAccess.requirePlanner(planId);
        return inviteRepository.findAllByPlanIdOrderByCreatedAtDesc(planId).stream().map(PlanInviteService::toDetails).toList();
    }

    @Transactional
    public IssuedPlanInvite create(UUID planId, PlanInviteCreateData data) {
        requireInviteManagementAccess(planId, data.type());
        shiftPlanService.requirePlan(planId);
        Instant now = clock.instant();
        if (data.expiresAt() != null && !data.expiresAt().isAfter(now)) {
            throw new ShiftValidationException("Invite expiry must be in the future");
        }
        validateRoleIds(planId, data.roleIds());

        String code = generateCode();
        PlanInviteEntity invite = new PlanInviteEntity();
        invite.setPlanId(planId);
        invite.setCode(code);
        invite.setType(data.type());
        invite.setActive(true);
        invite.setExpiresAt(data.expiresAt());
        invite.setUseLimit(data.useLimit());
        invite.setUses(0);
        invite.setCreatedAt(now);
        invite.setRoleIds(data.roleIds());
        invite = inviteRepository.save(invite);
        return new IssuedPlanInvite(invite.getId(), code);
    }

    @Transactional
    public void redeem(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new ShiftValidationException("Invite code is required");
        }
        PlanInviteEntity invite = inviteRepository.findByCodeForUpdate(rawCode.trim())
            .orElseThrow(() -> new ShiftNotFoundException("Invite not found"));
        Instant now = clock.instant();
        CurrentShiftActor.ShiftActor actor = planAccess.currentActor();
        volunteerProfileService.ensureProfile(actor.userId());
        var existingMembership = membershipRepository.findByPlanIdAndUserId(invite.getPlanId(), actor.userId());
        boolean isNewMembership = existingMembership.isEmpty();
        validateRedeemable(invite, now, isNewMembership);
        PlanMembershipEntity membership = existingMembership.orElseGet(() -> membershipRepository.save(
            new PlanMembershipEntity(invite.getPlanId(), actor.userId(), invite.getType() == PlanInviteType.PLANNER, now)));

        // Existing membership is idempotent and does not consume another use. A planner invite can elevate it.
        if (membership.getId() != null && !membership.isPlanner() && invite.getType() == PlanInviteType.PLANNER) {
            membership.grantPlanner();
        }
        membership.getRoleIds().addAll(invite.getRoleIds());

        if (isNewMembership) {
            invite.setUses(invite.getUses() + 1);
            if (invite.getUseLimit() != null && invite.getUses() >= invite.getUseLimit()) {
                invite.setActive(false);
                invite.setRevokedAt(now);
            }
        }
    }

    @Transactional
    public void revoke(UUID inviteId) {
        PlanInviteEntity invite = requireInvite(inviteId);
        requireInviteManagementAccess(invite.getPlanId(), invite.getType());
        if (invite.isActive()) {
            invite.setActive(false);
            invite.setRevokedAt(clock.instant());
        }
    }

    @Transactional
    public void delete(UUID inviteId) {
        PlanInviteEntity invite = requireInvite(inviteId);
        requireInviteManagementAccess(invite.getPlanId(), invite.getType());
        inviteRepository.delete(invite);
    }

    private void requireInviteManagementAccess(UUID planId, PlanInviteType type) {
        planAccess.requirePlanner(planId);
        if (type == PlanInviteType.PLANNER && !planAccess.currentActor().platformAdmin()) {
            throw new ShiftAccessDeniedException("Only a platform administrator can manage planner invites");
        }
    }

    private void validateRoleIds(UUID planId, Set<UUID> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        Collection<ShiftRoleEntity> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size() || roles.stream().anyMatch(role -> !planId.equals(role.getPlanId()))) {
            throw new ShiftValidationException("Invite roles must all belong to the shift plan");
        }
    }

    private void validateRedeemable(PlanInviteEntity invite, Instant now, boolean isNewMembership) {
        if (!invite.isActive()) {
            throw new ShiftConflictException("Invite is inactive");
        }
        if (invite.getExpiresAt() != null && !invite.getExpiresAt().isAfter(now)) {
            throw new ShiftConflictException("Invite has expired");
        }
        if (isNewMembership && invite.getUseLimit() != null && invite.getUses() >= invite.getUseLimit()) {
            throw new ShiftConflictException("Invite use limit has been reached");
        }
    }

    private PlanInviteEntity requireInvite(UUID inviteId) {
        return inviteRepository.findById(inviteId)
            .orElseThrow(() -> new ShiftNotFoundException("Plan invite not found: " + inviteId));
    }

    private static String generateCode() {
        byte[] bytes = new byte[INVITE_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static PlanInviteDetails toDetails(PlanInviteEntity invite) {
        return new PlanInviteDetails(invite.getId(), invite.getPlanId(), invite.getCode(), invite.getType(), invite.isActive(),
            invite.getExpiresAt(), invite.getUseLimit(), invite.getUses(), invite.getCreatedAt(), invite.getRevokedAt(),
            Set.copyOf(invite.getRoleIds()));
    }
}
