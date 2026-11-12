package at.conrunner.shift.endpoint.role;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.dto.role.RoleDto;
import at.conrunner.shift.dto.role.RoleModificationDto;
import at.conrunner.shift.service.RoleService;

@Tag(
    name = "role-endpoint"
)
@Slf4j
@RestController
@RequestMapping(
    value = "/api/v1/roles/{roleId}",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class RoleItemEndpoint {
    private final RoleService roleService;

    @GetMapping()
    @Operation(
        operationId = "getRole",
        description = "Get role by id"
    )
    public RoleDto getRole(@PathVariable String roleId) {
        return roleService.getRole(ConvertUtil.idToLong(roleId));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        operationId = "updateRole",
        description = "Update a role of this event"
    )
    public RoleDto updateRole(
        @PathVariable String roleId,
        @RequestBody RoleModificationDto role
    ) {
        return roleService.updateRole(ConvertUtil.idToLong(roleId), role);
    }

    @DeleteMapping()
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        operationId = "deleteRole",
        description = "Delete a role of this event"
    )
    public void deleteRole(@PathVariable String roleId) {
        roleService.deleteRole(ConvertUtil.idToLong(roleId));
    }
}
