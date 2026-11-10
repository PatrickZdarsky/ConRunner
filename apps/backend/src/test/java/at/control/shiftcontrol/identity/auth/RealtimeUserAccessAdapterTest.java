package at.control.shiftcontrol.identity.auth;

import at.control.shiftcontrol.identity.AuthenticatedActor;
import at.control.shiftcontrol.identity.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RealtimeUserAccessAdapterTest {
    @Test
    void resolvesLocalUserIdAndTokenExpiry() {
        var adapter = new RealtimeUserAccessAdapter(mock(UserService.class));
        var authentication = mock(Authentication.class);
        var actor = mock(AuthenticatedActor.class);
        var userId = UUID.randomUUID();
        var expiry = Instant.now().plusSeconds(60);
        var jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("external-subject").expiresAt(expiry).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(actor);
        when(authentication.getDetails()).thenReturn(jwt);
        when(actor.userId()).thenReturn(Optional.of(userId));
        assertThat(adapter.authenticate(authentication)).hasValueSatisfying(user -> {
            assertThat(user.userId()).isEqualTo(userId);
            assertThat(user.expiresAt()).isEqualTo(expiry);
        });
    }

    @Test
    void rejectsMachineActorsWithoutUserId() {
        var adapter = new RealtimeUserAccessAdapter(mock(UserService.class));
        var authentication = mock(Authentication.class);
        var actor = mock(AuthenticatedActor.class);
        var jwt = Jwt.withTokenValue("token").header("alg", "RS256").expiresAt(Instant.now().plusSeconds(60)).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(actor);
        when(authentication.getDetails()).thenReturn(jwt);
        when(actor.userId()).thenReturn(Optional.empty());
        assertThat(adapter.authenticate(authentication)).isEmpty();
    }
}
