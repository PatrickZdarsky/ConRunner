package at.control.shiftcontrol.communications.preference;

import at.control.shiftcontrol.identity.AuthenticatedActor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/me/notification-preferences/in-app", produces = "application/json")
class InAppPreferenceEndpoint {
    private final InAppPreferenceService preferences;
    InAppPreferenceEndpoint(InAppPreferenceService preferences) { this.preferences = preferences; }
    record Preference(Boolean enabled) {}

    @io.swagger.v3.oas.annotations.Operation(operationId = "getInAppPreference")
    @GetMapping("/{type}")
    Preference get(Authentication authentication, @PathVariable String type) {
        var id = userId(authentication);
        try { return new Preference(preferences.isEnabled(id, type)); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
    }

    @io.swagger.v3.oas.annotations.Operation(operationId = "updateInAppPreference")
    @PutMapping("/{type}")
    Preference put(Authentication authentication, @PathVariable String type, @RequestBody Preference preference) {
        if (preference == null || preference.enabled() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enabled state is required");
        try { preferences.setEnabled(userId(authentication), type, preference.enabled()); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
        return preference;
    }

    private UUID userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof AuthenticatedActor actor))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return actor.userId().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
