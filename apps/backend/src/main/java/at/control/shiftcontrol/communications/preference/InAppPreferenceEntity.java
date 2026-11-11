package at.control.shiftcontrol.communications.preference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "communications_in_app_preference")
class InAppPreferenceEntity {
    @Id UUID id;
    @Column(name = "recipient_id", nullable = false) UUID recipientId;
    @Column(name = "notification_type", nullable = false, length = 96) String type;
    @Column(nullable = false) boolean enabled;
    protected InAppPreferenceEntity() {}
}
