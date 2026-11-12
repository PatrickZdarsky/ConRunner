package at.conrunner.identity.auth;

import at.conrunner.identity.AuthenticatedActor;
import at.conrunner.identity.UserService;
import at.conrunner.platform.realtime.api.RealtimeUserAccess;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
class RealtimeUserAccessAdapter implements RealtimeUserAccess {
    private final UserService users;
    RealtimeUserAccessAdapter(UserService users) { this.users = users; }

    @Override
    public Optional<UserSession> authenticate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof AuthenticatedActor actor)
            || !(authentication.getDetails() instanceof Jwt jwt) || jwt.getExpiresAt() == null)
            return Optional.empty();
        return actor.userId().map(id -> new UserSession(id, jwt.getExpiresAt()));
    }

    @Override
    public boolean isActive(UUID userId) { return users.getUser(userId).isActive(); }
}
