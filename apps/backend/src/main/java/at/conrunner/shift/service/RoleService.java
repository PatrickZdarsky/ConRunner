package at.conrunner.shift.service;

import java.util.UUID;
import java.util.Collection;

import at.conrunner.shift.dto.role.RoleDto;
import at.conrunner.shift.dto.role.RoleModificationDto;
import at.conrunner.shift.dto.role.UserRoleAssignmentAssignDto;
import at.conrunner.shift.dto.userprofile.VolunteerDto;

import lombok.NonNull;

public interface RoleService {
    @NonNull Collection<RoleDto> getRoles(long shiftPlanId);

    @NonNull RoleDto getRole(long roleId);

    @NonNull RoleDto createRole(long shiftPlanId, @NonNull RoleModificationDto roleDto);

    @NonNull RoleDto updateRole(long roleId, @NonNull RoleModificationDto roleDto);

    void deleteRole(long roleId);

    @NonNull VolunteerDto createUserRoleAssignment(UUID userId, @NonNull UserRoleAssignmentAssignDto assignDto);

    void deleteUserRoleAssignment(UUID userId, long roleId);
}
