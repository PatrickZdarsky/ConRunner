package at.control.shiftcontrol.shift.service;

import java.util.Collection;

import at.control.shiftcontrol.shift.dto.role.RoleDto;
import at.control.shiftcontrol.shift.dto.role.RoleModificationDto;
import at.control.shiftcontrol.shift.dto.role.UserRoleAssignmentAssignDto;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;

import lombok.NonNull;

public interface RoleService {
    @NonNull Collection<RoleDto> getRoles(long shiftPlanId);

    @NonNull RoleDto getRole(long roleId);

    @NonNull RoleDto createRole(long shiftPlanId, @NonNull RoleModificationDto roleDto);

    @NonNull RoleDto updateRole(long roleId, @NonNull RoleModificationDto roleDto);

    void deleteRole(long roleId);

    @NonNull VolunteerDto createUserRoleAssignment(String userId, @NonNull UserRoleAssignmentAssignDto assignDto);

    void deleteUserRoleAssignment(String userId, long roleId);
}
