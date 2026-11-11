package at.control.shiftcontrol.communications.request;

import at.control.shiftcontrol.communications.api.NotificationContext;
import at.control.shiftcontrol.communications.preference.NotificationLanguageService;
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
    private final NotificationLanguageService languages;

    NotificationRequestWorker(NotificationRequestProcessingService processing, NotificationLanguageService languages) {
        this.processing = processing;
        this.languages = languages;
    }

    @Scheduled(fixedDelayString = "${shiftcontrol.communications.processing-delay:PT5S}")
    void process() {
        for (var request : processing.claim(25, Duration.ofMinutes(2))) {
            try {
                var context = NotificationContext.decode(request.templateData());
                var template = NotificationTemplates.render(request.type(), context, languages.languageFor(request.recipientId()));
                String link = NotificationLinks.forRequest(request.type(), context.target());
                processing.completeWithHistory(request, template.title(), template.body(), link);
            } catch (RuntimeException error) {
                log.warn("Notification request {} failed on attempt {}", request.id(), request.attempts(), error);
                processing.retry(request.id(), request.leaseId(), Instant.now().plusSeconds(60), 5);
            }
        }
    }

}
