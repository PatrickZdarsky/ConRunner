package at.control.shiftcontrol.shift.endpoint.user;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.dto.user.UserPlanDto;
import at.shiftcontrol.shiftservice.dto.user.UserPlanUpdateDto;
import at.shiftcontrol.shiftservice.service.UserAdministrationService;

@Tag(
    name = "user-plan-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/shift-plans/{shiftPlanId}/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserAdministrationItemEndpoint {
    private final UserAdministrationService service;

    @GetMapping()
    @Operation(
        operationId = "getUserOfPlan",
        description = "Get User"
    )
    public UserPlanDto getUserOfPlan(@PathVariable String shiftPlanId, @PathVariable String userId) {
        return service.getPlanUser(ConvertUtil.idToLong(shiftPlanId), userId);
    }

    @PatchMapping
    @Operation(
        operationId = "updateUserRoles",
        description = "Update user roles in a plan"
    )
    public UserPlanDto updateUserRoles(
        @PathVariable String shiftPlanId,
        @PathVariable String userId,
        @RequestBody @Valid UserPlanUpdateDto updateDto) {
        return service.updatePlanUser(ConvertUtil.idToLong(shiftPlanId), userId, updateDto);
    }
}
