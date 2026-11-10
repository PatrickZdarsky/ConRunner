package at.control.shiftcontrol.platform.realtime.api;

import org.springframework.security.core.Authentication;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Identity-owned adapter for authenticating and rechecking realtime users. */
public interface RealtimeUserAccess {
    record UserSession(UUID userId, Instant expiresAt) {}
    Optional<UserSession> authenticate(Authentication authentication);
    boolean isActive(UUID userId);
}
