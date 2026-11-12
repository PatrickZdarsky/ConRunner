package at.conrunner.shift.endpoint.positionslot;

import org.springframework.http.MediaType;
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
import at.conrunner.shift.dto.positionslot.PositionSlotDto;
import at.conrunner.shift.dto.positionslot.PositionSlotModificationDto;
import at.conrunner.shift.service.positionslot.PositionSlotService;

@Tag(
    name = "position-slot-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/shifts/{shiftId}/position-slots", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PositionSlotCollectionEndpoint {
    private final PositionSlotService positionSlotService;

    @PostMapping("/position-slot")
    @Operation(
        operationId = "createPositionSlotInShift",
        description = "Create position slot in a specific shift"
    )
    public PositionSlotDto createPositionSlot(@PathVariable String shiftId, @RequestBody @Valid PositionSlotModificationDto positionSlotModificationDto) {
        return positionSlotService.createPositionSlot(ConvertUtil.idToLong(shiftId), positionSlotModificationDto);
    }
}
