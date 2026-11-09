package at.control.shiftcontrol.shift.endpoint;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.dto.PretalxApiKeyDetailsDto;
import at.shiftcontrol.shiftservice.dto.PretalxApiKeyDto;
import at.shiftcontrol.shiftservice.sync.pretalx.PretalxApiKeyService;
import at.shiftcontrol.shiftservice.sync.pretalx.PretalxSyncService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/pretalx", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PretalxEndpoint {
    private final PretalxApiKeyService pretalxApiKeyService;
    private final PretalxSyncService pretalxSyncService;

    @GetMapping
    @AdminOnly
    @Operation(
        summary = "Get all Pretalx API keys",
        operationId = "getPretalxApiKeys",
        description = "Returns a list of all configured Pretalx API keys with their details."
    )
    public List<PretalxApiKeyDetailsDto> getApiKeys() {
        return pretalxApiKeyService.getAllKeys();
    }

    @PostMapping
    @AdminOnly
    @Operation(
        summary = "Add a new Pretalx API key",
        operationId = "addPretalxApiKey",
        description = "Adds a new Pretalx API key to the system. The key is validated, and if it is valid and has access to at least one event, it is saved."
    )
    public PretalxApiKeyDetailsDto addApiKey(@RequestBody PretalxApiKeyDto apiKeyDto) {
        return pretalxApiKeyService.addKey(apiKeyDto);
    }

    @DeleteMapping("/{apiKey}")
    @AdminOnly
    @Operation(
        summary = "Remove a Pretalx API key",
        operationId = "removePretalxApiKey",
        description = "Removes a Pretalx API key from the system."
    )
    public void removeApiKey(@PathVariable String apiKey) {
        pretalxApiKeyService.removeKey(apiKey);
    }

    @PostMapping("/sync")
    @AdminOnly
    @Operation(
        summary = "Trigger immediate synchronization with Pretalx",
        operationId = "syncPretalxNow",
        description = "Triggers an immediate synchronization with all configured Pretalx API keys."
    )
    public void syncNow() {
        pretalxSyncService.syncAll();
    }
}
