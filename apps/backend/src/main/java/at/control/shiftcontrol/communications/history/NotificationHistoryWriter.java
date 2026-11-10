package at.control.shiftcontrol.communications.history;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.UUID;

/** Internal communications operation; the caller's transaction also completes the request. */
@Service
public class NotificationHistoryWriter {
    private final NotificationRepository repository;
    private final NotificationRevisions revisions;
    NotificationHistoryWriter(NotificationRepository repository, NotificationRevisions revisions) {
        this.repository = repository;
        this.revisions = revisions;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(UUID requestId, UUID recipientId, String type, String title, String body, String link) {
        Objects.requireNonNull(requestId);
        Objects.requireNonNull(recipientId);
        if (type == null || !type.matches("[a-z][a-z0-9._-]{0,95}") || title == null || title.isBlank()
            || title.length() > 255 || body == null || body.isBlank() || body.length() > 2048)
            throw new IllegalArgumentException("Invalid notification content");
        if (link != null && (link.length() > 2048 || !link.startsWith("/") || link.startsWith("//")
            || link.contains("\\") || link.chars().anyMatch(Character::isISOControl)))
            throw new IllegalArgumentException("Invalid notification link");
        if (repository.insertIfAbsent(UUID.randomUUID(), requestId, recipientId, type, title, body, link) == 1)
            revisions.increment(recipientId);
    }
}
