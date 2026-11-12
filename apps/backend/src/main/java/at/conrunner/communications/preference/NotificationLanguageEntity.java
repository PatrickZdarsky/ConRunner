package at.conrunner.communications.preference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "communications_notification_language")
class NotificationLanguageEntity {
    @Id @Column(name = "recipient_id") UUID recipientId;
    @Column(nullable = false, length = 16) String language;
    protected NotificationLanguageEntity() {}
}
