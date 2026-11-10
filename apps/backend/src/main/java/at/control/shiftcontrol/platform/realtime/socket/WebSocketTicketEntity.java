package at.control.shiftcontrol.platform.realtime.socket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_websocket_ticket")
class WebSocketTicketEntity {
    @Id @Column(name = "ticket_hash", length = 64) String hash;
    @Column(name = "recipient_id", nullable = false) UUID recipientId;
    @Column(name = "expires_at", nullable = false) Instant expiresAt;
    @Column(name = "session_expires_at", nullable = false) Instant sessionExpiresAt;
    protected WebSocketTicketEntity() {}
    WebSocketTicketEntity(String hash, UUID recipientId, Instant expiresAt, Instant sessionExpiresAt) {
        this.hash = hash;
        this.recipientId = recipientId;
        this.expiresAt = expiresAt;
        this.sessionExpiresAt = sessionExpiresAt;
    }
}
