package at.control.shiftcontrol.communications.request;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "communications_request")
class NotificationRequestEntity {
    enum Status { PENDING, PROCESSING, COMPLETED, FAILED }

    @Id UUID id;
    @Column(name = "trigger_id", nullable = false) UUID triggerId;
    @Column(name = "recipient_id", nullable = false) UUID recipientId;
    @Column(name = "notification_type", nullable = false, length = 96) String type;
    @Column(name = "template_data", nullable = false, columnDefinition = "text") String templateData;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) Status status;
    @Column(nullable = false) int attempts;
    @Column(name = "available_at", nullable = false) Instant availableAt;
    @Column(name = "lease_id") UUID leaseId;
    @Column(name = "lease_until") Instant leaseUntil;
    @Column(name = "email_requested", nullable = false) boolean emailRequested;
    @Column(name = "in_app_requested", nullable = false) boolean inAppRequested;

    protected NotificationRequestEntity() {}

    void claim(UUID leaseId, Instant leaseUntil) {
        this.status = Status.PROCESSING;
        this.leaseId = leaseId;
        this.leaseUntil = leaseUntil;
        this.attempts++;
    }
}
