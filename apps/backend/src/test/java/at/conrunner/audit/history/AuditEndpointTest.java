package at.conrunner.audit.history;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditEndpointTest {
    AuditHistory history = mock(AuditHistory.class);
    AuditEndpoint endpoint = new AuditEndpoint(history);
    @Test void deniesOtherUsersAndInvalidRequests() {
        var user = new TestingAuthenticationToken("user", "", "user");
        assertThatThrownBy(() -> endpoint.search(user, 0, 25, null, null, null, null, null, null, null))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        var admin = new TestingAuthenticationToken("admin", "", "admin");
        assertThatThrownBy(() -> endpoint.search(admin, 0, 101, null, null, null, null, null, null, null))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(history);
    }
    @Test void dedicatedCapabilityCanSearch() {
        var viewer = new TestingAuthenticationToken("viewer", "", "audit.read");
        endpoint.search(viewer, 0, 25, null, null, null, "shift", null, null, null);
        verify(history).search(new AuditSearch(null, null, null, "shift", null, null, null), 0, 25);
    }
}
