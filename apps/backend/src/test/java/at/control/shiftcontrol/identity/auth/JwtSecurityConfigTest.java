package at.control.shiftcontrol.identity.auth;

import at.control.shiftcontrol.identity.ApplicationUser;
import at.control.shiftcontrol.identity.auth.JwtTokenDecoder.DecodedJwtToken;
import at.control.shiftcontrol.identity.config.props.OidcProviderProps;
import at.control.shiftcontrol.identity.service.ApplicationUserSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtSecurityConfigTest {
    private static final String ISSUER = "https://identity.example.test/realms/shiftcontrol";
    private static final String AUDIENCE = "shiftcontrol-backend";

    @Test
    void jwtValidator_acceptsValidTokens() {
        JwtSecurityConfig config = new JwtSecurityConfig(mock(ApplicationUserSyncService.class));
        Jwt validJwt = jwt(ISSUER, AUDIENCE, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));

        assertFalse(config.jwtValidator(properties()).validate(validJwt).hasErrors());
    }

    @Test
    void jwtValidator_rejectsExpiredTokens() {
        JwtSecurityConfig config = new JwtSecurityConfig(mock(ApplicationUserSyncService.class));
        Jwt expiredJwt = jwt(ISSUER, AUDIENCE, Instant.now().minusSeconds(120), Instant.now().minusSeconds(60));

        assertTrue(config.jwtValidator(properties()).validate(expiredJwt).hasErrors());
    }

    @Test
    void jwtValidator_rejectsTokensForAnotherAudience() {
        JwtSecurityConfig config = new JwtSecurityConfig(mock(ApplicationUserSyncService.class));
        Jwt wrongAudienceJwt = jwt(ISSUER, "another-api", Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));

        assertTrue(config.jwtValidator(properties()).validate(wrongAudienceJwt).hasErrors());
    }

    @Test
    void jwtAuthenticationConverter_rejectsSuspendedAccounts() {
        ApplicationUserSyncService syncService = mock(ApplicationUserSyncService.class);
        when(syncService.constructUserForLogin(org.mockito.ArgumentMatchers.any(DecodedJwtToken.class)))
            .thenReturn(new TestApplicationUser(false));
        JwtSecurityConfig config = new JwtSecurityConfig(syncService);

        assertThrows(DisabledException.class, () -> config.jwtAuthenticationConverter().convert(
            jwt(ISSUER, AUDIENCE, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60))));
    }

    private static OidcProviderProps properties() {
        return new OidcProviderProps(ISSUER, "https://identity.example.test/jwks", Set.of(ISSUER), AUDIENCE,
            "preferred_username");
    }

    private static Jwt jwt(String issuer, String audience, Instant issuedAt, Instant expiresAt) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer(issuer)
            .subject("subject-1")
            .audience(List.of(audience))
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }

    private static final class TestApplicationUser extends ApplicationUser {
        private TestApplicationUser(boolean enabled) {
            super(UUID.randomUUID(), List.<GrantedAuthority>of(), "pat", enabled);
        }
    }
}
