package at.control.shiftcontrol.shift.endpoint.shiftplan;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanInviteDetailsDto;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanJoinRequestDto;
import at.shiftcontrol.shiftservice.service.ShiftPlanService;

@Tag(
    name = "shift-plan-invite-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ShiftPlanInviteItemEndpoint {
    private final ShiftPlanService shiftPlanService;

    @PostMapping("/invites/{inviteId}")
    @Operation(
        operationId = "revokeShiftPlanInvite",
        description = "Revoke an invite code for a specific shift plan of an event (soft delete)"
    )
    public void revokeShiftPlanInvite(@PathVariable String inviteId) {
        shiftPlanService.revokeShiftPlanInvite(ConvertUtil.idToLong(inviteId));
    }

    @DeleteMapping("/invites/{inviteId}")
    @Operation(
        operationId = "deleteShiftPlanInvite",
        description = "Delete an invite code for a specific shift plan of an event (hard delete)"
    )
    public void deleteShiftPlanInvite(@PathVariable String inviteId) {
        shiftPlanService.deleteShiftPlanInvite(ConvertUtil.idToLong(inviteId));
    }


    @GetMapping("/invites/{inviteCode}")
    @Operation(
        operationId = "getShiftPlanInviteDetails",
        description = "Get details about a specific invite code for a shift plan"
    )
    public ShiftPlanInviteDetailsDto getShiftPlanInviteDetails(@PathVariable String inviteCode) {
        var temp = shiftPlanService.getShiftPlanInviteDetails(inviteCode);
        return temp;
    }

    @PostMapping("/join")
    @Operation(
        operationId = "joinShiftPlan",
        description = "Join a shift plan using an invite code"
    )
    public void joinShiftPlan(@RequestBody @Valid ShiftPlanJoinRequestDto requestDto) {
        shiftPlanService.joinShiftPlan(requestDto);
    }
}
