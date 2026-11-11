package at.control.shiftcontrol.communications.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "communications_notification_revision")
class NotificationRevisionEntity {
    @Id @Column(name = "recipient_id") UUID recipientId;
    @Column(nullable = false) long revision;
    protected NotificationRevisionEntity() {}
}
