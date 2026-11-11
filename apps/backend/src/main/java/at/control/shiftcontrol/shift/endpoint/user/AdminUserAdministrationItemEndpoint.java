package at.control.shiftcontrol.shift.endpoint.user;

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

import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dto.user.LockResetUserDto;
import at.control.shiftcontrol.shift.dto.user.UserEventDto;
import at.control.shiftcontrol.shift.dto.user.UserEventUpdateDto;
import at.control.shiftcontrol.shift.service.UserAdministrationService;

@Tag(
    name = "user-event-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AdminUserAdministrationItemEndpoint {
    private final UserAdministrationService service;

    @GetMapping()
    @Operation(
        operationId = "getUser",
        description = "Get User"
    )
    public UserEventDto getUser(@PathVariable UUID userId) {
        return service.getUser(userId);
    }

    @PatchMapping
    @Operation(
        operationId = "updateUserPlans",
        description = "Update user plans"
    )
    public UserEventDto updateUserPlans(
        @PathVariable UUID userId,
        @RequestBody @Valid UserEventUpdateDto updateDto) {
        return service.updateEventUser(userId, updateDto);
    }

    @PatchMapping("/lock")
    @Operation(
        operationId = "lockUserInPlan",
        description = "Lock a user in a given plan"
    )
    public UserEventDto lockUserForPlan(
        @PathVariable UUID userId,
        @RequestBody @Valid LockResetUserDto updateDto) {
        return service.lockUser(userId, ConvertUtil.idToLong(updateDto.getShiftPlanIds()));
    }

    @PatchMapping("/unlock")
    @Operation(
        operationId = "unlockUserInPlan",
        description = "Unlock a user in a given plan"
    )
    public UserEventDto unLockUserForPlan(
        @PathVariable UUID userId,
        @RequestBody @Valid LockResetUserDto updateDto) {
        return service.unLockUser(userId, ConvertUtil.idToLong(updateDto.getShiftPlanIds()));
    }

    @PatchMapping("/reset")
    @Operation(
        operationId = "resetUserInPlan",
        description = "Reset a user in a given plan"
    )
    public UserEventDto resetUserForPlan(
        @PathVariable UUID userId,
        @RequestBody @Valid LockResetUserDto updateDto) {
        return service.resetUser(userId, ConvertUtil.idToLong(updateDto.getShiftPlanIds()));
    }
}
