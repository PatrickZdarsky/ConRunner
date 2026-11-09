package at.control.shiftcontrol.shift.endpoint;

import at.shiftcontrol.lib.util.ConvertUtil;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.dto.PaginationDto;
import at.shiftcontrol.lib.dto.TrustAlertDto;
import at.shiftcontrol.shiftservice.dto.TrustAlertDisplayDto;
import at.shiftcontrol.shiftservice.service.TrustAlertService;

@RestController
@RequestMapping(value = "api/v1/trust-alerts", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TrustAlertEndpoint {
    private final TrustAlertService trustAlertService;

    @GetMapping
    @Operation(
        operationId = "getAllTrustAlerts",
        description = "Retrieves all trust alerts"
    )
    public PaginationDto<TrustAlertDisplayDto> getAllTrustAlerts(@RequestParam int page, @RequestParam int size) {
        return trustAlertService.getAllPaginated(page, size);
    }

    @PostMapping
    @Operation(
        operationId = "saveTrustAlert",
        description = "Saves the trust alert"
    )
    public TrustAlertDisplayDto saveTrustAlert(@RequestBody TrustAlertDto trustAlert) {
        return trustAlertService.save(trustAlert);
    }

    @DeleteMapping("/{id}")
    @Operation(
        operationId = "deleteTrustAlert",
        description = "Deletes the trust alert"
    )
    public void deleteTrustAlert(@PathVariable String id) {
        trustAlertService.delete(ConvertUtil.idToLong(id));
    }
}
