package at.control.shiftcontrol.communications.request;

import at.control.shiftcontrol.communications.api.NotificationTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
class NotificationRequestWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationRequestWorker.class);
    private final NotificationRequestProcessingService processing;

    NotificationRequestWorker(NotificationRequestProcessingService processing) { this.processing = processing; }

    @Scheduled(fixedDelayString = "${shiftcontrol.communications.processing-delay:PT5S}")
    void process() {
        for (var request : processing.claim(25, Duration.ofMinutes(2))) {
            try {
                var template = NotificationTemplates.get(request.type());
                String link;
                if (request.type().equals(NotificationTypes.TRUST_ALERT) && request.templateData().equals("global")) {
                    link = "/trust";
                } else {
                    if (!request.templateData().matches("[1-9][0-9]{0,18}"))
                        throw new IllegalArgumentException("Invalid event identifier");
                    link = "/events/" + request.templateData();
                }
                processing.completeWithHistory(request, template.title(), template.body(), link);
            } catch (RuntimeException error) {
                log.warn("Notification request {} failed on attempt {}", request.id(), request.attempts(), error);
                processing.retry(request.id(), request.leaseId(), Instant.now().plusSeconds(60), 5);
            }
        }
    }

}
