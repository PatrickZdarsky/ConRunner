package at.control.shiftcontrol.communications.email;

import at.control.shiftcontrol.communications.preference.EmailPreferenceService;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailDeliveryWorkerTest {
    private final EmailDeliveryService deliveries = mock(EmailDeliveryService.class);
    private final EmailPreferenceService preferences = mock(EmailPreferenceService.class);
    private final UserService users = mock(UserService.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final EmailDeliveryWorker worker = new EmailDeliveryWorker(deliveries, preferences, users, sender,
        "notifications@example.test", "https://app.example.test");
    private final EmailDeliveryService.Delivery delivery = new EmailDeliveryService.Delivery(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "shift.trade.requested", "Trade", "Requested", "/trades");

    @Test
    void doesNotSendWhenEmailIsNotVerified() {
        when(deliveries.claim()).thenReturn(List.of(delivery));
        when(users.getUser(delivery.recipientId())).thenReturn(User.builder().active(true).email("person@example.test").build());
        when(preferences.isEnabled(delivery.recipientId(), delivery.type())).thenReturn(true);
        worker.process();
        verify(deliveries).finish(delivery, true);
        verifyNoInteractions(sender);
    }

    @Test
    void retriesTransportFailure() {
        when(deliveries.claim()).thenReturn(List.of(delivery));
        when(users.getUser(delivery.recipientId())).thenReturn(User.builder().active(true).emailVerified(true).email("person@example.test").build());
        when(preferences.isEnabled(delivery.recipientId(), delivery.type())).thenReturn(true);
        doThrow(new MailSendException("temporary failure")).when(sender).send(any(SimpleMailMessage.class));
        worker.process();
        verify(deliveries).retry(delivery);
        verify(deliveries, never()).finish(any(), anyBoolean());
    }

    @Test
    void sendsVerifiedOptedInRecipient() {
        when(deliveries.claim()).thenReturn(List.of(delivery));
        when(users.getUser(delivery.recipientId())).thenReturn(User.builder().active(true).emailVerified(true).email("person@example.test").build());
        when(preferences.isEnabled(delivery.recipientId(), delivery.type())).thenReturn(true);
        when(deliveries.finish(delivery, false)).thenReturn(true);
        worker.process();
        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        org.assertj.core.api.Assertions.assertThat(message.getValue().getText()).contains("https://app.example.test/trades");
        verify(deliveries).finish(delivery, false);
    }
}
