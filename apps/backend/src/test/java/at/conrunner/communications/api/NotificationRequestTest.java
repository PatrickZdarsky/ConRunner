package at.conrunner.communications.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationRequestTest {
    @Test
    void rejectsUnstableNotificationType() {
        assertThatThrownBy(() -> new NotificationRequest(UUID.randomUUID(), UUID.randomUUID(), "Trade requested!", "{}"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresARecipient() {
        assertThatThrownBy(() -> new NotificationRequest(UUID.randomUUID(), null, "shift.trade.requested", "{}"))
            .isInstanceOf(NullPointerException.class);
    }
}
