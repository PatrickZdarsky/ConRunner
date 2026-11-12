package at.conrunner.shift.endpoint.shiftplan;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.conrunner.shift.dto.shiftplan.ShiftPlanDto;
import at.conrunner.shift.service.ShiftPlanService;

@Tag(
    name = "shift-plan-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/shift-plans", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ShiftPlanCollectionEndpoint {
    private final ShiftPlanService shiftPlanService;

    @GetMapping()
    @Operation(
        operationId = "getAllShiftPlans",
        description = "Find all existing shiftPlans"
    )
    public Collection<ShiftPlanDto> getAllShiftPlans() {
        return shiftPlanService.getAll();
    }

}
