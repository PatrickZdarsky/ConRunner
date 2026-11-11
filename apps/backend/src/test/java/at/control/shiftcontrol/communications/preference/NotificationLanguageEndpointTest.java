package at.control.shiftcontrol.communications.preference;

import at.control.shiftcontrol.identity.AuthenticatedActor;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationLanguageEndpointTest {
    @Test
    void updatesOnlyTheAuthenticatedUserAndRejectsUnsupportedLanguages() {
        var languages = mock(NotificationLanguageService.class);
        var endpoint = new NotificationLanguageEndpoint(languages);
        var auth = mock(Authentication.class);
        var actor = mock(AuthenticatedActor.class);
        var user = UUID.randomUUID();
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(actor);
        when(actor.userId()).thenReturn(Optional.of(user));
        endpoint.put(auth, new NotificationLanguageEndpoint.Language("de"));
        verify(languages).setLanguage(user, "de");
        doThrow(new IllegalArgumentException()).when(languages).setLanguage(user, "fr");
        assertThatThrownBy(() -> endpoint.put(auth, new NotificationLanguageEndpoint.Language("fr")))
            .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> endpoint.get(null)).isInstanceOf(ResponseStatusException.class);
    }
}
