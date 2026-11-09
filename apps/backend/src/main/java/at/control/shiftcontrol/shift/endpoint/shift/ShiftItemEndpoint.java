package at.control.shiftcontrol.shift.endpoint.shift;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.dto.shift.ShiftDetailsDto;
import at.shiftcontrol.shiftservice.dto.shift.ShiftDto;
import at.shiftcontrol.shiftservice.dto.shift.ShiftModificationDto;
import at.shiftcontrol.shiftservice.service.ShiftService;

@Tag(
    name = "shift-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/shifts/{shiftId}", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ShiftItemEndpoint {
    private final ApplicationUserProvider userProvider;
    private final ShiftService shiftService;

    @GetMapping()
    @Operation(
        operationId = "getShiftDetails",
        description = "Get details for a specific shift of a shift plan"
    )
    public ShiftDetailsDto getShiftDetails(@PathVariable String shiftId) {
        return shiftService.getShiftDetails(ConvertUtil.idToLong(shiftId), userProvider.getCurrentUser().getUserId());
    }

    @PutMapping()
    @Operation(
        operationId = "updateShift",
        description = "Update a specific shift of a shift plan"
    )
    public ShiftDto updateShift(@PathVariable String shiftId, @RequestBody @Valid ShiftModificationDto shiftModificationDto) {
        return shiftService.updateShift(ConvertUtil.idToLong(shiftId), shiftModificationDto);
    }

    @DeleteMapping()
    @Operation(
        operationId = "deleteShift",
        description = "Delete a specific shift of a shift plan"
    )
    public void deleteShift(@PathVariable String shiftId) {
        shiftService.deleteShift(ConvertUtil.idToLong(shiftId));
    }
}
