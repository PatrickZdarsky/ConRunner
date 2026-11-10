package at.control.shiftcontrol.communications.history;

import at.control.shiftcontrol.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, NotificationHistoryService.class, NotificationRevisions.class})
class NotificationHistoryPersistenceTest {
    @Autowired NotificationHistoryService history;
    @Autowired JdbcTemplate jdbc;
    @Autowired NotificationRevisions revisions;

    @Test
    void historyAndReadStateAreScopedToTheRecipient() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        UUID notification = UUID.randomUUID();
        jdbc.update("insert into communications_request (id, trigger_id, recipient_id, notification_type, template_data, created_at) values (?, ?, ?, 'shift.trade.requested', '{}', current_timestamp)", request, UUID.randomUUID(), owner);
        jdbc.update("insert into communications_notification (id, request_id, recipient_id, notification_type, title, body, created_at) values (?, ?, ?, 'shift.trade.requested', 'Trade', 'Requested', current_timestamp)", notification, request, owner);
        assertThat(history.list(other, 0, 10, null, null, null, null)).isEmpty();
        assertThat(history.setRead(other, notification, true)).isFalse();
        assertThat(history.list(owner, 0, 10, "shift.trade.requested", false, null, null)).hasSize(1);
        assertThat(history.setRead(owner, notification, true)).isTrue();
        assertThat(revisions.current(owner)).isEqualTo(1);
        assertThat(history.list(owner, 0, 10, null, true, null, null).getContent())
            .singleElement().satisfies(notificationResult -> assertThat(notificationResult.readAt()).isNotNull());
    }
}
