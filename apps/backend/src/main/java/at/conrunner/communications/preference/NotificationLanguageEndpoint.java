package at.conrunner.communications.preference;

import at.conrunner.identity.AuthenticatedActor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/me/notification-preferences/language", produces = "application/json")
class NotificationLanguageEndpoint {
    private final NotificationLanguageService languages;
    NotificationLanguageEndpoint(NotificationLanguageService languages) { this.languages = languages; }
    record Language(String language) {}

    @GetMapping
    Language get(Authentication authentication) {
        return new Language(languages.languageFor(userId(authentication)).toLanguageTag());
    }

    @PutMapping
    Language put(Authentication authentication, @RequestBody Language language) {
        var userId = userId(authentication);
        try {
            if (language == null || language.language() == null) throw new IllegalArgumentException("Language is required");
            languages.setLanguage(userId, language.language());
            return language;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private UUID userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof AuthenticatedActor actor))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return actor.userId().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
