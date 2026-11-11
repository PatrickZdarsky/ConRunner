package at.control.shiftcontrol.shift.config;

import at.control.shiftcontrol.identity.AuthenticatedActor;
import at.control.shiftcontrol.identity.Authorities;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestAuthFilterTest {
    @Test
    void createsAdminActorAndClearsAuthenticationAfterRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        var request = new MockHttpServletRequest();
        request.addHeader(TestSecurityConfig.HDR_TYPE, "ADMIN");
        request.addHeader(TestSecurityConfig.HDR_USERID, userId.toString());
        request.addHeader(TestSecurityConfig.HDR_USERNAME, "alice");

        new TestAuthFilter().doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var actor = (AuthenticatedActor) authentication.getPrincipal();
            assertThat(actor.userId()).contains(userId);
            assertThat(actor.name()).isEqualTo("alice");
            assertThat(actor.authorities()).extracting("authority").containsExactly(Authorities.PLATFORM_ADMIN_ROLE);
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
