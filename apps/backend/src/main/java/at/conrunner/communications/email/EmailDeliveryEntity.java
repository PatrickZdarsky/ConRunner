package at.conrunner.communications.email;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "communications_email_delivery", uniqueConstraints = @UniqueConstraint(
    name = "uq_communications_email_delivery_request", columnNames = "request_id"))
class EmailDeliveryEntity {
    enum Status { PENDING, PROCESSING, SENT, FAILED, CANCELED }
    @Id UUID id;
    @Column(name = "request_id", nullable = false) UUID requestId;
    @Column(name = "recipient_id", nullable = false) UUID recipientId;
    @Column(name = "notification_type", nullable = false, length = 96) String type;
    @Column(nullable = false, length = 255) String title;
    @Column(nullable = false, length = 2048) String body;
    @Column(name = "link_target", length = 2048) String linkTarget;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) Status status;
    @Column(nullable = false) int attempts;
    @Column(name = "available_at", nullable = false) Instant availableAt;
    @Column(name = "lease_id") UUID leaseId;
    @Column(name = "lease_until") Instant leaseUntil;
    protected EmailDeliveryEntity() {}

    void claim(UUID leaseId, Instant leaseUntil) {
        this.status = Status.PROCESSING;
        this.leaseId = leaseId;
        this.leaseUntil = leaseUntil;
        this.attempts++;
    }
}
