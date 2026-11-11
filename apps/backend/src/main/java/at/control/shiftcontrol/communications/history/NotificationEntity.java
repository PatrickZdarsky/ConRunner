package at.control.shiftcontrol.communications.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "communications_notification")
class NotificationEntity {
    @Id UUID id;
    @Column(name = "request_id", nullable = false) UUID requestId;
    @Column(name = "recipient_id", nullable = false) UUID recipientId;
    @Column(name = "notification_type", nullable = false, length = 96) String type;
    @Column(nullable = false, length = 255) String title;
    @Column(nullable = false, length = 2048) String body;
    @Column(name = "link_target", length = 2048) String linkTarget;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "read_at") Instant readAt;

    protected NotificationEntity() {}
}
