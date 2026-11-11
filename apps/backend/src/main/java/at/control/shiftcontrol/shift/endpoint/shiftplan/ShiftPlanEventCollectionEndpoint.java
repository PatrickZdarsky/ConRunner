package at.control.shiftcontrol.shift.endpoint.shiftplan;

import java.util.Collection;

import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanCreateDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanModificationDto;
import at.control.shiftcontrol.shift.service.ShiftPlanService;
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
    name = "shift-plan-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/events/{eventId}/shift-plans", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ShiftPlanEventCollectionEndpoint {
    private final ShiftPlanService shiftPlanService;

    @GetMapping()
    @Operation(
        operationId = "getAllShiftPlansOfEvent",
        description = "Find all (volunteer related) shiftPlans of an event"
    )
    public Collection<ShiftPlanDto> getAllShiftPlansOfEvent(@PathVariable String eventId) {
        return shiftPlanService.getAllOfEvent(ConvertUtil.idToLong(eventId));
    }
    //Todo: Add search capability in future

    @PostMapping()
    @Operation(
        operationId = "createShiftPlan",
        description = "Create a new shiftPlan"
    )
    public ShiftPlanCreateDto createShiftPlan(@PathVariable String eventId, @Valid @RequestBody ShiftPlanModificationDto modificationDto) {
        return shiftPlanService.createShiftPlan(ConvertUtil.idToLong(eventId), modificationDto);
    }
}
