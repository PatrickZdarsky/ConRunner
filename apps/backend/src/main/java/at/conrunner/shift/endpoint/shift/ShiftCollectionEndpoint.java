package at.conrunner.shift.endpoint.shift;

import java.util.List;
import org.springframework.http.MediaType;
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

import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.dto.shift.ShiftDto;
import at.conrunner.shift.dto.shift.ShiftModificationDto;
import at.conrunner.shift.service.ShiftService;

@Tag(
    name = "shift-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/shift-plans/{shiftPlanId}/shifts", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ShiftCollectionEndpoint {
    private final ShiftService shiftService;

    @PostMapping()
    @Operation(
        operationId = "createShift",
        description = "Create a new shift in a specific shift plan"
    )
    public ShiftDto createShift(@PathVariable String shiftPlanId, @RequestBody @Valid ShiftModificationDto shiftModificationDto) {
        return shiftService.createShift(ConvertUtil.idToLong(shiftPlanId), shiftModificationDto);
    }

    @GetMapping()
    @Operation(
        operationId = "getShifts",
        description = "Get all shift ids of a specific shift plan"
    )
    public List<Long> getShifts(@PathVariable String shiftPlanId) {
        return shiftService.getShiftIds(ConvertUtil.idToLong(shiftPlanId));
    }
}
