package at.control.shiftcontrol.shift.endpoint.positionslot;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.auth.ApplicationUserProvider;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotJoinErrorDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotModificationDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotPreferenceDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotPreferenceUpdateDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotRequestDto;
import at.control.shiftcontrol.shift.service.positionslot.PositionSlotService;

@Tag(
    name = "position-slot-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/position-slots/{positionSlotId}", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PositionSlotItemEndpoint {
    private final PositionSlotService positionSlotService;
    private final ApplicationUserProvider userProvider;

    @GetMapping
    @Operation(
        operationId = "getPositionSlot",
        description = "Get details for a specific position slot in a shift"
    )
    public PositionSlotDto getPositionSlot(@PathVariable String positionSlotId) {
        return positionSlotService.findById(ConvertUtil.idToLong(positionSlotId));
    }

    @GetMapping("/assignment")
    @Operation(
        operationId = "getPositionSlotUserAssignment",
        description = "Get the assignment for a specific position slot in a shift for the current user"
    )
    public AssignmentDto getPositionSlotUserAssignment(@PathVariable String positionSlotId) {
        return positionSlotService.getUserAssignment(ConvertUtil.idToLong(positionSlotId), userProvider.getCurrentUser().getUserId());
    }

    @PutMapping
    @Operation(
        operationId = "updatePositionSlot",
        description = "Update a specific position slot in a shift"
    )
    public PositionSlotDto updatePositionSlot(@PathVariable String positionSlotId, @RequestBody @Valid PositionSlotModificationDto modificationDto) {
        return positionSlotService.updatePositionSlot(ConvertUtil.idToLong(positionSlotId), modificationDto);
    }

    @DeleteMapping
    @Operation(
        operationId = "deletePositionSlot",
        description = "Delete a specific position slot in a shift"
    )
    public void deletePositionSlot(@PathVariable String positionSlotId) {
        positionSlotService.deletePositionSlot(ConvertUtil.idToLong(positionSlotId));
    }

    @PostMapping("/join")
    @Operation(
        operationId = "joinPositionSlot",
        description = "Join a specific position slot",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successfully joined the position slot",
                content = @Content(
                    schema = @Schema(implementation = AssignmentDto.class)
                )
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Conflict - e.g., position slot is already full",
                content = @Content(
                    schema = @Schema(implementation = PositionSlotJoinErrorDto.class)
                )
            )
        }
    )
    public AssignmentDto joinPositionSlot(@PathVariable String positionSlotId, @RequestBody @Valid PositionSlotRequestDto requestDto) {
        return positionSlotService.join(
            ConvertUtil.idToLong(positionSlotId),
            userProvider.getCurrentUser().getUserId(),
            requestDto
        );
    }

    @DeleteMapping("/leave")
    @Operation(
        operationId = "leavePositionSlot",
        description = "Leave a specific position slot",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successfully left the position slot"
            )
        }
    )
    public void leavePositionSlot(@PathVariable String positionSlotId) {
        positionSlotService.leave(
            ConvertUtil.idToLong(positionSlotId),
            userProvider.getCurrentUser().getUserId());
    }

    @PostMapping("/join-request")
    @Operation(
        operationId = "joinRequestPositionSlot",
        description = "Request to join a specific position slot",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successfully requested to join the position slot",
                content = @Content(
                    schema = @Schema(implementation = AssignmentDto.class)
                )
            )
        }
    )
    public AssignmentDto joinRequestPositionSlot(@PathVariable String positionSlotId) {
        return positionSlotService.joinRequest(
            ConvertUtil.idToLong(positionSlotId),
            userProvider.getCurrentUser().getUserId()
        );
    }

    @PatchMapping("/leave-request")
    @Operation(
        operationId = "leaveRequestPositionSlot",
        description = "Request to leave a specific position slot",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successfully requested to leave the position slot"
            )
        }
    )
    public void leaveRequestPositionSlot(@PathVariable String positionSlotId) {
        positionSlotService.leaveRequest(
            ConvertUtil.idToLong(positionSlotId),
            userProvider.getCurrentUser().getUserId());
    }


    @DeleteMapping("/join-request-withdraw")
    @Operation(
        operationId = "joinRequestWithdrawPositionSlot",
        description = "Withdraw a request to join a specific position slot",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successfully cancelled request to join the position slot"
            )
        }
    )
    public void joinRequestWithdrawPositionSlot(@PathVariable String positionSlotId) {
        positionSlotService.joinRequestWithdraw(
            ConvertUtil.idToLong(positionSlotId),
            userProvider.getCurrentUser().getUserId()
        );
    }

    @PatchMapping("/leave-request-withdraw")
    @Operation(
        operationId = "leaveRequestWithdrawPositionSlot",
        description = "Withdraw a request to leave a specific position slot",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successfully cancelled request to leave the position slot"
            )
        }
    )
    public void leaveRequestWithdrawPositionSlot(@PathVariable String positionSlotId) {
        positionSlotService.leaveRequestWithdraw(
            ConvertUtil.idToLong(positionSlotId),
            userProvider.getCurrentUser().getUserId());
    }

    @PutMapping("/preference")
    @Operation(
        operationId = "setPositionSlotPreference",
        description = "Set preference for a specific position slot"
    )
    public PositionSlotPreferenceDto setPositionSlotPreference(
        @PathVariable String positionSlotId,
        @RequestBody @Valid PositionSlotPreferenceUpdateDto preferenceUpdateDto
    ) {
        positionSlotService.setPreference(
            userProvider.getCurrentUser().getUserId(),
            ConvertUtil.idToLong(positionSlotId),
            preferenceUpdateDto.getPreferenceValue()
        );
        return PositionSlotPreferenceDto.builder().preferenceValue(preferenceUpdateDto.getPreferenceValue()).build();
    }

    @PatchMapping("/auction")
    @Operation(
        operationId = "auctionAssignment",
        description = "Put the logged in users assignment for the PositionSlot up for auction"
    )
    public AssignmentDto auctionAssignment(@PathVariable String positionSlotId) {
        return positionSlotService.createAuction(
            ConvertUtil.idToLong(positionSlotId),
            userProvider.getCurrentUser().getUserId());
    }

    @PutMapping("/claim-auction/{offeringUserId}")
    @Operation(
        operationId = "claimAuction",
        description = "Assign the logged in user to the auctions PositionSlot"
    )
    public AssignmentDto claimAssignment(
        @PathVariable String positionSlotId,
        @PathVariable String offeringUserId,
        @RequestBody @Valid PositionSlotRequestDto requestDto
    ) {
        return positionSlotService.claimAuction(
            ConvertUtil.idToLong(positionSlotId), offeringUserId,
            userProvider.getCurrentUser().getUserId(),
            requestDto);
    }

    @PatchMapping("/cancel-auction")
    @Operation(
        operationId = "cancelAuction",
        description = "Cancel the logged in users auction for the PositionSlot"
    )
    public AssignmentDto cancelAuction(@PathVariable String positionSlotId) {
        return positionSlotService.cancelAuction(
            ConvertUtil.idToLong(positionSlotId),
            userProvider.getCurrentUser().getUserId());
    }
}
