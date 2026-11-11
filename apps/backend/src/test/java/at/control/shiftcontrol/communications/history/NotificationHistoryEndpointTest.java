package at.control.shiftcontrol.communications.history;

import at.control.shiftcontrol.identity.AuthenticatedActor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class NotificationHistoryEndpointTest {
    private final NotificationHistoryService service = mock(NotificationHistoryService.class);
    private final NotificationHistoryEndpoint endpoint = new NotificationHistoryEndpoint(service);

    @Test
    void usesAuthenticatedUserForReadChanges() {
        UUID user = UUID.randomUUID();
        UUID notification = UUID.randomUUID();
        var authentication = mock(Authentication.class);
        var actor = mock(AuthenticatedActor.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(actor);
        when(actor.userId()).thenReturn(Optional.of(user));
        when(service.setRead(user, notification, true)).thenReturn(true);
        endpoint.setRead(authentication, notification, new NotificationHistoryEndpoint.ReadState(true));
        verify(service).setRead(user, notification, true);
    }

    @Test
    void rejectsMachineWithoutUserId() {
        var authentication = mock(Authentication.class);
        var actor = mock(AuthenticatedActor.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(actor);
        when(actor.userId()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> endpoint.list(authentication, 0, 25, null, null, null, null))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> org.assertj.core.api.Assertions.assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(service);
    }
}
