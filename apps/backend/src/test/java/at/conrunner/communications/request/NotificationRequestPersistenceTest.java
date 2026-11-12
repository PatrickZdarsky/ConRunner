package at.conrunner.communications.request;

import at.conrunner.TestcontainersConfiguration;
import at.conrunner.communications.api.NotificationRequest;
import at.conrunner.communications.api.NotificationRequests;
import at.conrunner.communications.history.NotificationHistoryWriter;
import at.conrunner.communications.history.NotificationRevisions;
import at.conrunner.communications.email.EmailDeliveryService;
import at.conrunner.communications.preference.EmailPreferenceService;
import at.conrunner.communications.preference.InAppPreferenceService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestcontainersConfiguration.class, NotificationRequestService.class, NotificationRequestProcessingService.class, NotificationHistoryWriter.class, NotificationRevisions.class, EmailDeliveryService.class})
class NotificationRequestPersistenceTest {
    @Autowired NotificationRequests requests;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired NotificationRequestProcessingService queue;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @MockitoBean EmailPreferenceService preferences;
    @MockitoBean InAppPreferenceService inAppPreferences;

    @org.junit.jupiter.api.BeforeEach
    void enableInAppByDefault() {
        org.mockito.Mockito.when(inAppPreferences.isEnabled(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
    }

    @Test
    void duplicateRequestCreatesOneRow() {
        var request = new NotificationRequest(UUID.randomUUID(), UUID.randomUUID(), "shift.trade.requested", "{}");
        requests.request(request);
        requests.request(request);
        assertThat(jdbc.queryForObject("select count(*) from communications_request where trigger_id = ?", Integer.class, request.triggerId())).isEqualTo(1);
    }

    @Test
    void requestRollsBackWithItsDomainTransaction() {
        UUID trigger = UUID.randomUUID();
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            requests.request(new NotificationRequest(trigger, UUID.randomUUID(), "shift.trade.requested", "{}"));
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from communications_request where trigger_id = ?", Integer.class, trigger)).isZero();
    }

    @Test
    void expiredLeaseCanBeReclaimedAndOldWorkerCannotComplete() {
        UUID trigger = UUID.randomUUID();
        requests.request(new NotificationRequest(trigger, UUID.randomUUID(), "shift.trade.requested", "{}"));
        var first = queue.claim(1, java.time.Duration.ofMinutes(1)).getFirst();
        jdbc.update("update communications_request set lease_until = current_timestamp - interval '1 second' where id = ?", first.id());
        entityManager.clear();
        var second = queue.claim(1, java.time.Duration.ofMinutes(1)).getFirst();
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.attempts()).isEqualTo(2);
        assertThat(queue.complete(first.id(), first.leaseId())).isFalse();
        assertThat(queue.complete(second.id(), second.leaseId())).isTrue();
    }

    @Test
    void exhaustedRequestIsNotClaimedAgain() {
        requests.request(new NotificationRequest(UUID.randomUUID(), UUID.randomUUID(), "shift.trade.requested", "{}"));
        var claimed = queue.claim(1, java.time.Duration.ofMinutes(1)).getFirst();
        assertThat(queue.retry(claimed.id(), claimed.leaseId(), java.time.Instant.now(), 1)).isTrue();
        assertThat(queue.claim(1, java.time.Duration.ofMinutes(1))).isEmpty();
    }

    @Test
    void completionCreatesRecipientHistory() {
        UUID recipient = UUID.randomUUID();
        requests.request(new NotificationRequest(UUID.randomUUID(), recipient, "shift.trade.requested", "{}"));
        var claimed = queue.claim(1, java.time.Duration.ofMinutes(1)).getFirst();
        queue.completeWithHistory(claimed, "Trade requested", "A trade was requested.", "/trades");
        assertThat(jdbc.queryForObject("select count(*) from communications_notification where request_id = ? and recipient_id = ?",
            Integer.class, claimed.id(), recipient)).isEqualTo(1);
    }

    @Test
    void optedInRequestCreatesOneEmailDeliveryAlongsideHistory() {
        UUID recipient = UUID.randomUUID();
        org.mockito.Mockito.when(preferences.isEnabled(recipient, "shift.trade.requested")).thenReturn(true);
        requests.request(new NotificationRequest(UUID.randomUUID(), recipient, "shift.trade.requested", "{}"));
        var claimed = queue.claim(1, java.time.Duration.ofMinutes(1)).getFirst();
        queue.completeWithHistory(claimed, "Trade requested", "A trade was requested.", "/trades");
        assertThat(jdbc.queryForObject("select count(*) from communications_email_delivery where request_id = ? and recipient_id = ?",
            Integer.class, claimed.id(), recipient)).isEqualTo(1);
    }

    @Test
    void disabledInAppPreferenceDoesNotCreateHistory() {
        UUID recipient = UUID.randomUUID();
        org.mockito.Mockito.when(inAppPreferences.isEnabled(recipient, "shift.trade.requested")).thenReturn(false);
        requests.request(new NotificationRequest(UUID.randomUUID(), recipient, "shift.trade.requested", "{}"));
        var claimed = queue.claim(1, java.time.Duration.ofMinutes(1)).getFirst();
        queue.completeWithHistory(claimed, "Trade requested", "A trade was requested.", "/events/1");
        assertThat(jdbc.queryForObject("select count(*) from communications_notification where request_id = ?", Integer.class, claimed.id())).isZero();
    }
}
