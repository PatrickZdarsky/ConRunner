package at.conrunner.shift.mapper;

import lombok.NoArgsConstructor;

import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.ShiftPlanInvite;
import at.conrunner.shift.dto.invite.ShiftPlanInviteDto;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class InviteMapper {
    public static ShiftPlanInviteDto toInviteDto(ShiftPlanInvite invite, ShiftPlan shiftPlan) {
        return ShiftPlanInviteDto.builder()
            .id(String.valueOf(invite.getId()))
            .code(invite.getCode())
            .type(invite.getType())
            .shiftPlanDto(ShiftPlanMapper.toShiftPlanDto(shiftPlan))
            .active(invite.isActive())
            .expiresAt(invite.getExpiresAt())
            .maxUses(invite.getMaxUses())
            .usedCount(invite.getUses())
            .autoAssignedRoles(invite.getAutoAssignRoles() == null ? null : RoleMapper.toRoleDto(invite.getAutoAssignRoles()))
            .createdAt(invite.getCreatedAt())
            .revokedAt(invite.getRevokedAt())
            .build();
    }
}
