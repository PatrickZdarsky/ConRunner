package at.control.shiftcontrol.communications.email;

import at.control.shiftcontrol.communications.preference.EmailPreferenceService;
import at.control.shiftcontrol.identity.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "shiftcontrol.communications.email", name = "enabled", havingValue = "true")
class EmailDeliveryWorker {
    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryWorker.class);
    private final EmailDeliveryService deliveries;
    private final EmailPreferenceService preferences;
    private final UserService users;
    private final JavaMailSender sender;
    private final String from;
    private final java.net.URI frontendBase;

    EmailDeliveryWorker(EmailDeliveryService deliveries, EmailPreferenceService preferences, UserService users,
                        JavaMailSender sender, @Value("${shiftcontrol.communications.email.from}") String from,
                        @Value("${shiftcontrol.communications.email.frontend-base-url}") String frontendBaseUrl) {
        this.deliveries = deliveries;
        this.preferences = preferences;
        this.users = users;
        this.sender = sender;
        if (!EmailAddress.isValid(from)) throw new IllegalArgumentException("Valid email sender address is required");
        this.from = from;
        this.frontendBase = java.net.URI.create(frontendBaseUrl);
        if (!"https".equalsIgnoreCase(frontendBase.getScheme()) || frontendBase.getHost() == null
            || frontendBase.getUserInfo() != null || frontendBase.getQuery() != null || frontendBase.getFragment() != null)
            throw new IllegalArgumentException("An HTTPS frontend base URL is required");
    }

    @Scheduled(fixedDelayString = "${shiftcontrol.communications.email.processing-delay:PT5S}")
    void process() {
        for (var delivery : deliveries.claim()) {
            try {
                var user = users.getUser(delivery.recipientId());
                if (!preferences.isEnabled(delivery.recipientId(), delivery.type()) || !user.isActive() || !user.isEmailVerified()
                    || !EmailAddress.isValid(user.getEmail())) {
                    deliveries.finish(delivery, true);
                    continue;
                }
                var message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(user.getEmail());
                message.setSubject(delivery.title());
                message.setText(delivery.body() + (delivery.link() == null ? "" : "\n\n" + frontendBase.resolve(delivery.link())));
                sender.send(message);
                if (!deliveries.finish(delivery, false))
                    log.warn("Email delivery {} finished after its lease expired", delivery.id());
            } catch (RuntimeException error) {
                // Avoid recording mail addresses or provider error text in durable state or logs.
                log.warn("Email delivery {} failed; scheduling retry", delivery.id());
                deliveries.retry(delivery);
            }
        }
    }
}
