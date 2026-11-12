package at.conrunner.identity.auth;

import at.conrunner.identity.ApplicationUser;
import at.conrunner.identity.AuthenticatedActor;
import at.conrunner.identity.ActorType;
import at.conrunner.identity.auth.JwtTokenDecoder.DecodedJwtToken;
import at.conrunner.identity.config.props.MachineClientRegistration;
import at.conrunner.identity.config.props.OidcProviderProps;
import at.conrunner.identity.service.ApplicationUserSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtSecurityConfigTest {
    private static final String ISSUER = "https://identity.example.test/realms/shiftcontrol";
    private static final String AUDIENCE = "shiftcontrol-backend";

    @Test
    void jwtValidator_acceptsValidTokens() {
        JwtSecurityConfig config = securityConfig(mock(ApplicationUserSyncService.class));
        Jwt validJwt = jwt(ISSUER, AUDIENCE, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));

        assertFalse(config.jwtValidator(properties()).validate(validJwt).hasErrors());
    }

    @Test
    void jwtValidator_rejectsExpiredTokens() {
        JwtSecurityConfig config = securityConfig(mock(ApplicationUserSyncService.class));
        Jwt expiredJwt = jwt(ISSUER, AUDIENCE, Instant.now().minusSeconds(120), Instant.now().minusSeconds(60));

        assertTrue(config.jwtValidator(properties()).validate(expiredJwt).hasErrors());
    }

    @Test
    void jwtValidator_rejectsTokensForAnotherAudience() {
        JwtSecurityConfig config = securityConfig(mock(ApplicationUserSyncService.class));
        Jwt wrongAudienceJwt = jwt(ISSUER, "another-api", Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));

        assertTrue(config.jwtValidator(properties()).validate(wrongAudienceJwt).hasErrors());
    }

    @Test
    void jwtAuthenticationConverter_rejectsSuspendedAccounts() {
        ApplicationUserSyncService syncService = mock(ApplicationUserSyncService.class);
        when(syncService.constructUserForLogin(org.mockito.ArgumentMatchers.any(DecodedJwtToken.class)))
            .thenReturn(new TestApplicationUser(false));
        JwtSecurityConfig config = securityConfig(syncService);

        assertThrows(DisabledException.class, () -> config.jwtAuthenticationConverter(properties()).convert(
            jwt(ISSUER, AUDIENCE, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60))));
    }

    @Test
    void jwtAuthenticationConverter_exposesHumanActor() {
        ApplicationUserSyncService syncService = mock(ApplicationUserSyncService.class);
        TestApplicationUser user = new TestApplicationUser(true);
        when(syncService.constructUserForLogin(org.mockito.ArgumentMatchers.any(DecodedJwtToken.class)))
            .thenReturn(user);
        Jwt jwt = jwt(ISSUER, AUDIENCE, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));

        var authentication = securityConfig(syncService).jwtAuthenticationConverter(properties()).convert(jwt);

        assertSame(user, authentication.getPrincipal());
        assertEquals(ActorType.HUMAN, user.type());
        assertFalse(user.isMachine());
        assertEquals(user.getUserId(), user.userId().orElseThrow());
        assertEquals("pat", authentication.getName());
        assertEquals(user.authorities(), authentication.getAuthorities());
        assertSame(jwt, authentication.getDetails());
    }

    @Test
    void jwtAuthenticationConverter_usesOnlyConfiguredMachineAuthorities() {
        ApplicationUserSyncService syncService = mock(ApplicationUserSyncService.class);
        MachineClientAuthorizer authorizer = new MachineClientAuthorizer(List.of(
            new MachineClientRegistration("notification-worker", Set.of("notifications.recipients.read"))));
        JwtSecurityConfig config = new JwtSecurityConfig(syncService, authorizer);
        Jwt machineJwt = Jwt.withTokenValue("machine-token")
            .header("alg", "RS256")
            .issuer(ISSUER)
            .subject("service-account-notification-worker")
            .audience(List.of(AUDIENCE))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("client_id", "notification-worker")
            .claim("scope", "notifications.recipients.read untrusted.extra")
            .build();

        var authentication = config.jwtAuthenticationConverter(properties()).convert(machineJwt);

        AuthenticatedActor actor = (AuthenticatedActor) authentication.getPrincipal();
        assertEquals(ActorType.MACHINE, actor.type());
        assertTrue(actor.isMachine());
        assertTrue(actor.userId().isEmpty());
        assertEquals("notification-worker", actor.name());
        assertEquals(actor.name(), authentication.getName());
        assertEquals(actor.authorities(), authentication.getAuthorities());
        assertSame(machineJwt, authentication.getDetails());
        assertEquals(List.of("notifications.recipients.read"),
            authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        verifyNoInteractions(syncService);
    }

    @Test
    void jwtAuthenticationConverter_rejectsMachineTokenWithoutEveryConfiguredAuthority() {
        MachineClientAuthorizer authorizer = new MachineClientAuthorizer(List.of(
            new MachineClientRegistration("notification-worker", Set.of("notifications.recipients.read"))));
        JwtSecurityConfig config = new JwtSecurityConfig(mock(ApplicationUserSyncService.class), authorizer);
        Jwt machineJwt = Jwt.withTokenValue("machine-token")
            .header("alg", "RS256")
            .issuer(ISSUER)
            .subject("service-account-notification-worker")
            .audience(List.of(AUDIENCE))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("client_id", "notification-worker")
            .claim("scope", "untrusted.extra")
            .build();

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
            () -> config.jwtAuthenticationConverter(properties()).convert(machineJwt));
    }

    private static JwtSecurityConfig securityConfig(ApplicationUserSyncService syncService) {
        return new JwtSecurityConfig(syncService, new MachineClientAuthorizer(List.of()));
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
