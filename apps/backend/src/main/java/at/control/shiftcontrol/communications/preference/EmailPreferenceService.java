package at.control.shiftcontrol.communications.preference;

import at.control.shiftcontrol.identity.UserService;
import at.control.shiftcontrol.communications.email.EmailAddress;
import at.control.shiftcontrol.communications.request.NotificationTemplates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class EmailPreferenceService {
    private final EmailPreferenceRepository repository;
    private final UserService users;
    EmailPreferenceService(EmailPreferenceRepository repository, UserService users) {
        this.repository = repository;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId, String type) {
        if (!NotificationTemplates.supports(type)) throw new IllegalArgumentException("Unsupported notification type");
        return repository.findByRecipientIdAndType(userId, type).map(p -> p.enabled).orElse(false);
    }

    @Transactional
    public void setEnabled(UUID userId, String type, boolean enabled) {
        if (!NotificationTemplates.supports(type)) throw new IllegalArgumentException("Unsupported notification type");
        if (enabled) {
            var user = users.getUser(userId);
            if (!user.isActive() || !user.isEmailVerified() || !EmailAddress.isValid(user.getEmail()))
                throw new IllegalArgumentException("A verified email address is required");
        }
        repository.setEnabled(UUID.randomUUID(), userId, type, enabled);
    }
}
