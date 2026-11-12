package at.conrunner.communications.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationRequestTest {
    @Test
    void rejectsUnstableNotificationType() {
        var trigger = UUID.randomUUID();
        var recipient = UUID.randomUUID();
        assertThatThrownBy(() -> new NotificationRequest(trigger, recipient, "Trade requested!", "{}"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresARecipient() {
        var trigger = UUID.randomUUID();
        assertThatThrownBy(() -> new NotificationRequest(trigger, null, "shift.trade.requested", "{}"))
            .isInstanceOf(NullPointerException.class);
    }
}
