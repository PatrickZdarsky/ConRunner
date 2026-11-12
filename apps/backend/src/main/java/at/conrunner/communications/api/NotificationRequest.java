package at.conrunner.communications.api;

import java.util.Objects;
import java.util.UUID;

/** A request for one recipient. The trigger identifies a particular domain occurrence. */
public record NotificationRequest(UUID triggerId, UUID recipientId, String type, String templateData) {
    public NotificationRequest {
        Objects.requireNonNull(triggerId, "triggerId");
        Objects.requireNonNull(recipientId, "recipientId");
        if (type == null || !type.matches("[a-z][a-z0-9._-]{0,95}")) {
            throw new IllegalArgumentException("Invalid notification type");
        }
        Objects.requireNonNull(templateData, "templateData");
    }
}
