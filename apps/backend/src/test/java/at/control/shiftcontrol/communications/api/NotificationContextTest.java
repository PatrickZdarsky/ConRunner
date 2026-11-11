package at.control.shiftcontrol.communications.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class NotificationContextTest {
    @Test
    void roundTripsAndBoundsUserProvidedNames() {
        var data = NotificationContext.encode("42:7", "Front\ndesk", "x".repeat(300));
        var context = NotificationContext.decode(data);
        assertThat(context.target()).isEqualTo("42:7");
        assertThat(context.subject()).isEqualTo("Front desk");
        assertThat(context.detail()).hasSize(255);
        assertThat(NotificationContext.decode("42").target()).isEqualTo("42");
        assertThatIllegalArgumentException().isThrownBy(() -> NotificationContext.decode("v1.invalid"));
    }
}
