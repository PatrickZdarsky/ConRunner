package at.control.shiftcontrol.shift.endpoint.shiftplan;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanModificationDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanPatchStatusDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanStatisticsDto;
import at.control.shiftcontrol.shift.service.ShiftPlanService;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@Tag(
    name = "shift-plan-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/shift-plans/{shiftPlanId}", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ShiftPlanItemEndpoint {
    private final ShiftPlanService shiftPlanService;

    @GetMapping()
    @Operation(
        operationId = "getShiftPlan",
        description = "Find shiftPlans by its id"
    )
    public ShiftPlanDto getShiftPlan(@PathVariable String shiftPlanId) {
        return shiftPlanService.get(ConvertUtil.idToLong(shiftPlanId));
    }

    @PutMapping()
    @Operation(
        operationId = "updateShiftPlan",
        description = "Update an existing shiftPlan"
    )
    public ShiftPlanDto updateShiftPlan(
        @PathVariable String shiftPlanId,
        @Valid @RequestBody ShiftPlanModificationDto modificationDto
    ) {
        return shiftPlanService.update(ConvertUtil.idToLong(shiftPlanId), modificationDto);
    }

    @DeleteMapping()
    @Operation(
        operationId = "deleteShiftPlan",
        description = "Delete an existing shiftPlan"
    )
    public void deleteShiftPlan(@PathVariable String shiftPlanId) {
        shiftPlanService.delete(ConvertUtil.idToLong(shiftPlanId));
    }

    @PatchMapping("/status")
    @Operation(
        operationId = "editLockStatus",
        description = "Edit the LockState for a shift plan"
    )
    @ResponseStatus(NO_CONTENT)
    public void patchState(@PathVariable String shiftPlanId, @Valid @RequestBody ShiftPlanPatchStatusDto requestDto) {
        shiftPlanService.updateLockStatus(ConvertUtil.idToLong(shiftPlanId), requestDto.getLockStatus());
    }

    @PostMapping("/leave")
    @Operation(
        operationId = "leaveShiftPlan",
        description = "Leave a shift plan and remove all existing assignments"
    )
    @ResponseStatus(NO_CONTENT)
    public void leavePlan(@PathVariable String shiftPlanId) {
        shiftPlanService.leavePlan(ConvertUtil.idToLong(shiftPlanId));
    }

    @GetMapping("/stats")
    @Operation(
        operationId = "getShiftPlanStatistics",
        description = "Get statistics of the shift plan and its volunteers"
    )
    public ShiftPlanStatisticsDto getStatistics(@PathVariable String shiftPlanId) {
        return shiftPlanService.getShiftPlanStatistics(ConvertUtil.idToLong(shiftPlanId));
    }
}
