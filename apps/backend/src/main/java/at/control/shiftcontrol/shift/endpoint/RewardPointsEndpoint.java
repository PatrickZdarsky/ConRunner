package at.control.shiftcontrol.shift.endpoint;

import java.util.Collection;

import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.lib.exception.NotFoundException;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dto.rewardpoints.EventPointsDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsExportDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsShareTokenCreateRequestDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsShareTokenDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.TotalPointsDto;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsExportService;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsLedgerService;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/reward-points", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RewardPointsEndpoint {
    private final RewardPointsLedgerService rewardPointsLedgerService;
    private final RewardPointsService rewardPointsService;
    private final RewardPointsExportService rewardPointsExportService;
    private final CurrentShiftActor currentActor;

    @GetMapping()
    @Operation(
        operationId = "getCurrentUserTotalRewardPoints",
        description = "Get total reward points of the current user"
    )
    public TotalPointsDto getCurrentUserTotalRewardPoints() throws NotFoundException {
        var actor = currentActor.get();
        return rewardPointsLedgerService.getTotalPoints(actor.requireUserId().toString());
    }

    @GetMapping("/events")
    @Operation(
        operationId = "getCurrentUserEventRewardPoints",
        description = "Get reward points per event for the current user"
    )
    public Collection<EventPointsDto> getCurrentUserEventRewardPoints() throws NotFoundException {
        var actor = currentActor.get();
        return rewardPointsLedgerService.getPointsGroupedByEvent(actor.requireUserId().toString());
    }

    @GetMapping("/events/{eventId}")
    @Operation(
        operationId = "getCurrentUserRewardPointsForEvent",
        description = "Get reward points for a specific event for the current user"
    )
    public EventPointsDto getCurrentUserRewardPointsForEvent(@PathVariable long eventId) throws NotFoundException {
        var actor = currentActor.get();
        return rewardPointsLedgerService.getPointsForEvent(actor.requireUserId().toString(), eventId);
    }

    @GetMapping("/export")
    @Operation(
        operationId = "exportRewardPoints",
        description = "Export reward points for all users"
    )
    public ResponseEntity<Resource> exportRewardPoints() {
        var export = rewardPointsExportService.exportRewardPoints();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + export.getFileName())
            // filename will be set in frontend regardless of this value because header value is not used, but it is good practice to set it here anyway
            .contentType(export.getMediaType())
            .body(new InputStreamResource(export.getExportStream()));
    }

    @GetMapping("/share")
    @Operation(
        operationId = "getAllRewardPointsShareTokens",
        description = "List all share tokens for reward points"
    )
    public Collection<RewardPointsShareTokenDto> getAllRewardPointsShareTokens() {
        return rewardPointsService.getAllRewardPointsShareTokens();
    }

    @GetMapping("/share/{shareToken}")
    @Operation(
        operationId = "getRewardPointsWithShareToken",
        description = "Get reward points using a share token"
    )
    public Collection<RewardPointsExportDto> getRewardPointsWithShareToken(@PathVariable String shareToken) throws NotFoundException {
        return rewardPointsService.getRewardPointsWithShareToken(shareToken);
    }


    @PostMapping("/share")
    @Operation(
        operationId = "createRewardPointsShareToken",
        description = "Create a share token for reward points"
    )
    public RewardPointsShareTokenDto createRewardPointsShareToken(@RequestBody @Valid RewardPointsShareTokenCreateRequestDto requestDto)
        throws NotFoundException {
        return rewardPointsService.createRewardPointsShareToken(requestDto);
    }

    @DeleteMapping("/share/{tokenId}")
    @Operation(
        operationId = "deleteRewardPointsShareToken",
        description = "Delete a share token for reward points"
    )
    public void deleteRewardPointsShareToken(@PathVariable String tokenId) {
        rewardPointsService.deleteRewardPointsShareToken(ConvertUtil.idToLong(tokenId));
    }
}
