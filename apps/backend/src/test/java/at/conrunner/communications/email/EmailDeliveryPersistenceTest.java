package at.conrunner.communications.email;

import at.conrunner.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, EmailDeliveryService.class})
class EmailDeliveryPersistenceTest {
    @Autowired EmailDeliveryService deliveries;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void deliveryLeaseCanBeReclaimedAndOldLeaseCannotComplete() {
        UUID request = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        jdbc.update("insert into communications_request (id, trigger_id, recipient_id, notification_type, template_data, created_at) values (?, ?, ?, 'shift.trade.requested', '{}', current_timestamp)", request, UUID.randomUUID(), recipient);
        deliveries.enqueue(request, recipient, "shift.trade.requested", "Trade", "Requested", "/trades");
        var first = deliveries.claim().getFirst();
        jdbc.update("update communications_email_delivery set lease_until = current_timestamp - interval '1 second' where id = ?", first.id());
        entityManager.clear();
        var second = deliveries.claim().getFirst();
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(deliveries.finish(first, false)).isFalse();
        assertThat(deliveries.finish(second, false)).isTrue();
    }
}
