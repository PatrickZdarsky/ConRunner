package at.control.shiftcontrol.shift.plan;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShiftRoleService {
    private final ShiftRoleRepository roleRepository;
    private final PlanMembershipRepository membershipRepository;
    private final PlanInviteRepository inviteRepository;
    private final PlanAccess planAccess;
    private final Clock clock;

    public Collection<ShiftRoleDetails> list(UUID planId) {
        planAccess.requireMember(planId);
        return roleRepository.findAllByPlanIdOrderByNameAsc(planId).stream().map(ShiftRoleService::toDetails).toList();
    }

    @Transactional
    public ShiftRoleDetails create(UUID planId, ShiftRoleData data) {
        planAccess.requirePlanner(planId);
        if (roleRepository.existsByPlanIdAndNameIgnoreCase(planId, data.name())) {
            throw new ShiftConflictException("A role with this name already exists in the shift plan");
        }
        ShiftRoleEntity role = new ShiftRoleEntity();
        role.setPlanId(planId);
        apply(data, role);
        role.setCreatedAt(clock.instant());
        return toDetails(roleRepository.save(role));
    }

    @Transactional
    public ShiftRoleDetails update(UUID roleId, ShiftRoleData data) {
        ShiftRoleEntity role = requireRole(roleId);
        planAccess.requirePlanner(role.getPlanId());
        roleRepository.findByPlanIdAndNameIgnoreCase(role.getPlanId(), data.name())
            .filter(existing -> !existing.getId().equals(roleId))
            .ifPresent(_ -> {
                throw new ShiftConflictException("A role with this name already exists in the shift plan");
            });
        apply(data, role);
        return toDetails(roleRepository.save(role));
    }

    @Transactional
    public void delete(UUID roleId) {
        ShiftRoleEntity role = requireRole(roleId);
        planAccess.requirePlanner(role.getPlanId());
        membershipRepository.findAllByPlanId(role.getPlanId())
            .forEach(membership -> membership.getRoleIds().remove(roleId));
        inviteRepository.findAllByPlanIdOrderByCreatedAtDesc(role.getPlanId())
            .forEach(invite -> invite.getRoleIds().remove(roleId));
        roleRepository.delete(role);
    }

    @Transactional
    public void assignToMember(UUID roleId, UUID userId) {
        ShiftRoleEntity role = requireRole(roleId);
        CurrentShiftActor.ShiftActor actor = planAccess.currentActor();
        if (!role.isSelfAssignable() || !actor.userId().equals(userId)) {
            planAccess.requirePlanner(role.getPlanId());
        } else {
            planAccess.requireMember(role.getPlanId());
        }
        PlanMembershipEntity membership = requireMembership(role.getPlanId(), userId);
        if (!membership.getRoleIds().add(roleId)) {
            throw new ShiftConflictException("Role is already assigned to this plan member");
        }
    }

    @Transactional
    public void removeFromMember(UUID roleId, UUID userId) {
        ShiftRoleEntity role = requireRole(roleId);
        CurrentShiftActor.ShiftActor actor = planAccess.currentActor();
        if (!role.isSelfAssignable() || !actor.userId().equals(userId)) {
            planAccess.requirePlanner(role.getPlanId());
        } else {
            planAccess.requireMember(role.getPlanId());
        }
        PlanMembershipEntity membership = requireMembership(role.getPlanId(), userId);
        if (!membership.getRoleIds().remove(roleId)) {
            throw new ShiftNotFoundException("Role is not assigned to this plan member");
        }
    }

    ShiftRoleEntity requireRole(UUID roleId) {
        return roleRepository.findById(roleId)
            .orElseThrow(() -> new ShiftNotFoundException("Shift role not found: " + roleId));
    }

    private PlanMembershipEntity requireMembership(UUID planId, UUID userId) {
        if (userId == null) {
            throw new ShiftValidationException("Plan member user ID is required");
        }
        return membershipRepository.findByPlanIdAndUserId(planId, userId)
            .orElseThrow(() -> new ShiftNotFoundException("Plan membership not found"));
    }

    private static void apply(ShiftRoleData data, ShiftRoleEntity role) {
        role.setName(data.name());
        role.setDescription(data.description());
        role.setSelfAssignable(data.selfAssignable());
    }

    static ShiftRoleDetails toDetails(ShiftRoleEntity role) {
        return new ShiftRoleDetails(role.getId(), role.getPlanId(), role.getName(), role.getDescription(), role.isSelfAssignable());
    }
}
