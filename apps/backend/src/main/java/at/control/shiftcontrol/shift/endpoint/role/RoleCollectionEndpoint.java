package at.control.shiftcontrol.shift.endpoint.role;

import java.util.Collection;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dto.role.RoleDto;
import at.control.shiftcontrol.shift.dto.role.RoleModificationDto;
import at.control.shiftcontrol.shift.service.RoleService;


@Tag(
    name = "role-endpoint"
)
@Slf4j
@RestController
@RequestMapping(
    value = "/api/v1/shift-plans/{shiftPlanId}/roles",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class RoleCollectionEndpoint {
    private final RoleService roleService;

    @GetMapping
    @Operation(
        operationId = "getRoles",
        description = "Get roles of this event"
    )
    public Collection<RoleDto> getRoles(@PathVariable String shiftPlanId) {
        return roleService.getRoles(ConvertUtil.idToLong(shiftPlanId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        operationId = "createRole",
        description = "Create a new role for an event"
    )
    public RoleDto createRole(
        @PathVariable String shiftPlanId,
        @RequestBody @Valid RoleModificationDto role
    ) {
        return roleService.createRole(ConvertUtil.idToLong(shiftPlanId), role);
    }
}
