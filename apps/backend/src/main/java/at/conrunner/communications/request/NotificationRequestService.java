package at.conrunner.communications.request;

import at.conrunner.communications.api.NotificationRequest;
import at.conrunner.communications.api.NotificationRequests;
import at.conrunner.communications.preference.EmailPreferenceService;
import at.conrunner.communications.preference.InAppPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
class NotificationRequestService implements NotificationRequests {
    private final NotificationRequestRepository repository;
    private final EmailPreferenceService preferences;
    private final InAppPreferenceService inAppPreferences;

    NotificationRequestService(NotificationRequestRepository repository, EmailPreferenceService preferences, InAppPreferenceService inAppPreferences) {
        this.repository = repository;
        this.preferences = preferences;
        this.inAppPreferences = inAppPreferences;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void request(NotificationRequest request) {
        Objects.requireNonNull(request, "request");
        repository.insertIfAbsent(UUID.randomUUID(), request.triggerId(), request.recipientId(), request.type(),
            request.templateData(), preferences.isEnabled(request.recipientId(), request.type()),
            inAppPreferences.isEnabled(request.recipientId(), request.type()));
    }
}
