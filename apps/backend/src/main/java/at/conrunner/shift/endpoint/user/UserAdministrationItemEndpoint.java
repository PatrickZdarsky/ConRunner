package at.conrunner.shift.endpoint.user;

import java.util.UUID;
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

import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.dto.user.UserPlanDto;
import at.conrunner.shift.dto.user.UserPlanUpdateDto;
import at.conrunner.shift.service.UserAdministrationService;

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
    public UserPlanDto getUserOfPlan(@PathVariable String shiftPlanId, @PathVariable UUID userId) {
        return service.getPlanUser(ConvertUtil.idToLong(shiftPlanId), userId);
    }

    @PatchMapping
    @Operation(
        operationId = "updateUserRoles",
        description = "Update user roles in a plan"
    )
    public UserPlanDto updateUserRoles(
        @PathVariable String shiftPlanId,
        @PathVariable UUID userId,
        @RequestBody @Valid UserPlanUpdateDto updateDto) {
        return service.updatePlanUser(ConvertUtil.idToLong(shiftPlanId), userId, updateDto);
    }
}
