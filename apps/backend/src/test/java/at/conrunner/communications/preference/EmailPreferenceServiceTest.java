package at.conrunner.communications.preference;

import at.conrunner.identity.User;
import at.conrunner.identity.UserService;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailPreferenceServiceTest {
    private final EmailPreferenceRepository repository = mock(EmailPreferenceRepository.class);
    private final UserService users = mock(UserService.class);
    private final EmailPreferenceService preferences = new EmailPreferenceService(repository, users);

    @Test
    void missingPreferenceIsDisabled() {
        assertThat(preferences.isEnabled(UUID.randomUUID(), "shift.trade.requested")).isFalse();
    }

    @Test
    void enablingRequiresActiveVerifiedEmail() {
        var userId = UUID.randomUUID();
        when(users.getUser(userId)).thenReturn(User.builder().active(true).email("person@example.test").build());
        assertThatThrownBy(() -> preferences.setEnabled(userId, "shift.trade.requested", true))
            .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).setEnabled(any(), any(), any(), anyBoolean());
    }

    @Test
    void verifiedUserCanOptIn() {
        var userId = UUID.randomUUID();
        when(users.getUser(userId)).thenReturn(User.builder().active(true).emailVerified(true).email("person@example.test").build());
        preferences.setEnabled(userId, "shift.trade.requested", true);
        verify(repository).setEnabled(any(), eq(userId), eq("shift.trade.requested"), eq(true));
    }

    @Test
    void verifiedButMalformedAddressCannotOptIn() {
        var userId = UUID.randomUUID();
        when(users.getUser(userId)).thenReturn(User.builder().active(true).emailVerified(true).email("a@example.test\r\nBcc: x@example.test").build());
        assertThatThrownBy(() -> preferences.setEnabled(userId, "shift.trade.requested", true))
            .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).setEnabled(any(), any(), any(), anyBoolean());
    }
}
