package at.control.shiftcontrol.shift.endpoint.role;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dto.role.UserRoleAssignmentAssignDto;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;
import at.control.shiftcontrol.shift.service.RoleService;

@Slf4j
@RestController
@RequestMapping(
    value = "/api/v1/users/{userId}/role-assignments",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class RoleUserAssignmentEndpoint {
    private final RoleService roleService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        operationId = "createRoleAssignment",
        description = "Create a role assignment for a user in an event"
    )
    public VolunteerDto createUserRoleAssignment(
        @PathVariable UUID userId,
        @RequestBody @Valid UserRoleAssignmentAssignDto roleAssignmentAssign
    ) {
        return roleService.createUserRoleAssignment(userId, roleAssignmentAssign);
    }

    @DeleteMapping("/{roleId}")
    @Operation(
        operationId = "deleteRoleAssignment",
        description = "Delete a role assignment for a user in an event"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserRoleAssignment(
        @PathVariable UUID userId,
        @PathVariable String roleId
    ) {
        roleService.deleteUserRoleAssignment(userId, ConvertUtil.idToLong(roleId));
    }
}
