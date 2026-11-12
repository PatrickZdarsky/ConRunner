package at.conrunner.communications.preference;

import at.conrunner.communications.request.NotificationTemplates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class InAppPreferenceService {
    private final InAppPreferenceRepository repository;
    InAppPreferenceService(InAppPreferenceRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId, String type) {
        if (!NotificationTemplates.supports(type)) throw new IllegalArgumentException("Unsupported notification type");
        return repository.findByRecipientIdAndType(userId, type).map(p -> p.enabled).orElse(true);
    }

    @Transactional
    public void setEnabled(UUID userId, String type, boolean enabled) {
        if (!NotificationTemplates.supports(type)) throw new IllegalArgumentException("Unsupported notification type");
        repository.setEnabled(UUID.randomUUID(), userId, type, enabled);
    }
}
