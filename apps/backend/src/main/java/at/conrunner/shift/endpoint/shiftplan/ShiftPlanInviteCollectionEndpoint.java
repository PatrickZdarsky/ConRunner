package at.conrunner.shift.endpoint.shiftplan;

import java.util.Collection;

import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.dto.invite.ShiftPlanInviteCreateRequestDto;
import at.conrunner.shift.dto.invite.ShiftPlanInviteCreateResponseDto;
import at.conrunner.shift.dto.invite.ShiftPlanInviteDto;
import at.conrunner.shift.service.ShiftPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "shift-plan-invite-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/shift-plans/{shiftPlanId}/invites", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ShiftPlanInviteCollectionEndpoint {
    private final ShiftPlanService shiftPlanService;

    // endpoint to list all codes for shiftPlan
    @GetMapping()
    @Operation(
        operationId = "getAllShiftPlanInvites",
        description = "List all invite codes for a specific shift plan of an event"
    )
    public Collection<ShiftPlanInviteDto> getAllShiftPlanInvites(@PathVariable String shiftPlanId) {
        return shiftPlanService.getAllShiftPlanInvites(ConvertUtil.idToLong(shiftPlanId));
    }

    @PostMapping()
    @Operation(
        operationId = "createShiftPlanInvite",
        description = "Create an invite code for a specific shift plan of an event"
    )
    public ShiftPlanInviteCreateResponseDto createShiftPlanInvite(
        @PathVariable String shiftPlanId,
        @RequestBody @Valid ShiftPlanInviteCreateRequestDto requestDto
    ) {
        return shiftPlanService.createShiftPlanInviteCode(ConvertUtil.idToLong(shiftPlanId), requestDto);
    }
}
