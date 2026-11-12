package at.conrunner.communications.preference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "communications_in_app_preference", uniqueConstraints = @UniqueConstraint(
    name = "uq_communications_in_app_preference_user_type", columnNames = {"recipient_id", "notification_type"}))
class InAppPreferenceEntity {
    @Id UUID id;
    @Column(name = "recipient_id", nullable = false) UUID recipientId;
    @Column(name = "notification_type", nullable = false, length = 96) String type;
    @Column(nullable = false) boolean enabled;
    protected InAppPreferenceEntity() {}
}
