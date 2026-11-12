package at.conrunner.shift.mapper;

import java.util.Collection;
import java.util.Collections;


import lombok.NonNull;

import at.conrunner.shift.entity.Role;
import at.conrunner.shift.dto.role.RoleDto;
import at.conrunner.shift.dto.role.RoleModificationDto;

public class RoleMapper {
    private RoleMapper() {
    }

    public static RoleDto toRoleDto(@NonNull Role role) {
        return new RoleDto(
            String.valueOf(role.getId()),
            role.getName(),
            role.getDescription(),
            role.isSelfAssignable(),
            role.getRewardPointsPerMinute()
        );
    }

    public static Collection<RoleDto> toRoleDto(Collection<Role> roles) {
        if (roles == null) {
            return Collections.emptyList();
        }
        return roles.stream()
            .map(RoleMapper::toRoleDto)
            .toList();
    }

    public static Collection<RoleDto> toRoleDto(Collection<Role> roles, Long shiftPlanId) {
        if (roles == null) {
            return Collections.emptyList();
        }
        return roles.stream()
            .filter(x -> x.getShiftPlan().getId() == shiftPlanId)
            .map(RoleMapper::toRoleDto)
            .toList();
    }

    public static Role toRole(@NonNull RoleModificationDto roleDto) {
        return Role.builder()
            .name(roleDto.getName())
            .description(roleDto.getDescription())
            .selfAssignable(roleDto.isSelfAssignable())
            .rewardPointsPerMinute(roleDto.getRewardPointsPerMinute())
            .build();
    }
}
