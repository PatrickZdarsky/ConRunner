package at.control.shiftcontrol.communications.preference;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InAppPreferenceServiceTest {
    private final InAppPreferenceRepository repository = mock(InAppPreferenceRepository.class);
    private final InAppPreferenceService preferences = new InAppPreferenceService(repository);

    @Test
    void inAppIsEnabledByDefault() {
        assertThat(preferences.isEnabled(UUID.randomUUID(), "shift.trade.requested")).isTrue();
    }

    @Test
    void supportsDisablingAMigratedType() {
        var user = UUID.randomUUID();
        preferences.setEnabled(user, "shift.assignment.leave.accepted", false);
        verify(repository).setEnabled(any(), eq(user), eq("shift.assignment.leave.accepted"), eq(false));
    }

    @Test
    void rejectsUnknownTypes() {
        assertThatThrownBy(() -> preferences.setEnabled(UUID.randomUUID(), "unknown", true))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
}
