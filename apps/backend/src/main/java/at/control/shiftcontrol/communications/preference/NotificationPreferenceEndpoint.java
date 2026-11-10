package at.control.shiftcontrol.communications.preference;

import at.control.shiftcontrol.communications.request.NotificationTemplates;
import at.control.shiftcontrol.identity.AuthenticatedActor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/me/notification-preferences")
class NotificationPreferenceEndpoint {
    private final EmailPreferenceService email;
    private final InAppPreferenceService inApp;
    NotificationPreferenceEndpoint(EmailPreferenceService email, InAppPreferenceService inApp) {
        this.email = email;
        this.inApp = inApp;
    }
    record Preference(String type, String label, boolean inApp, boolean email) {}

    @GetMapping
    List<Preference> list(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof AuthenticatedActor actor))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var id = actor.userId().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        return NotificationTemplates.types().stream()
            .map(type -> new Preference(type, NotificationTemplates.get(type).title(),
                inApp.isEnabled(id, type), email.isEnabled(id, type))).toList();
    }
}
