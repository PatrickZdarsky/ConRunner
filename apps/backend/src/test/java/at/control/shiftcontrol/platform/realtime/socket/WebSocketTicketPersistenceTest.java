package at.control.shiftcontrol.platform.realtime.socket;

import at.control.shiftcontrol.TestcontainersConfiguration;
import at.control.shiftcontrol.platform.realtime.api.RealtimeUserAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({TestcontainersConfiguration.class, WebSocketTicketService.class})
class WebSocketTicketPersistenceTest {
    @Autowired WebSocketTicketService tickets;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @MockitoBean RealtimeUserAccess users;

    @Test
    void ticketIsHashedAndCanOnlyBeUsedOnce() {
        var userId = UUID.randomUUID();
        when(users.isActive(userId)).thenReturn(true);
        var ticket = tickets.issue(userId, java.time.Instant.now().plusSeconds(120));
        assertThat(jdbc.queryForObject("select count(*) from platform_websocket_ticket where ticket_hash = ?", Integer.class, ticket)).isZero();
        assertThat(tickets.consume(ticket)).hasValueSatisfying(identity -> assertThat(identity.userId()).isEqualTo(userId));
        assertThat(tickets.consume(ticket)).isEmpty();
    }

    @Test
    void expiredTicketCannotAuthenticate() {
        var userId = UUID.randomUUID();
        when(users.isActive(userId)).thenReturn(true);
        var ticket = tickets.issue(userId, java.time.Instant.now().plusSeconds(120));
        entityManager.flush();
        jdbc.update("update platform_websocket_ticket set expires_at = current_timestamp - interval '1 second'");
        entityManager.clear();
        assertThat(tickets.consume(ticket)).isEmpty();
    }
}
