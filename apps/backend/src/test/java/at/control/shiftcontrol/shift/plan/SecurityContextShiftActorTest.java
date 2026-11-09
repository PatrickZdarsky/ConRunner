package at.control.shiftcontrol.shift.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import at.control.shiftcontrol.identity.ApplicationUser;
import at.control.shiftcontrol.identity.Authorities;
import at.control.shiftcontrol.shift.error.ShiftAccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextShiftActorTest {
    private final SecurityContextShiftActor actor = new SecurityContextShiftActor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adaptsTheIdentityApplicationUserWithoutReadingProviderClaims() {
        UUID userId = UUID.randomUUID();
        GrantedAuthority authority = new SimpleGrantedAuthority(Authorities.PLATFORM_ADMIN_ROLE);
        ApplicationUser user = new TestApplicationUser(userId, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, "token", user.getAuthorities()));

        CurrentShiftActor.ShiftActor result = actor.current();

        assertEquals(userId, result.userId());
        assertEquals(true, result.platformAdmin());
    }

    @Test
    void rejectsAPrincipalThatIsNotAnIdentityApplicationUser() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("legacy-provider-user", "token"));

        assertThrows(ShiftAccessDeniedException.class, actor::current);
    }

    private static final class TestApplicationUser extends ApplicationUser {
        private TestApplicationUser(UUID userId, List<? extends GrantedAuthority> authorities) {
            super(userId, authorities, "test-user", true);
        }
    }
}
