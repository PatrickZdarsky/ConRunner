package at.control.shiftcontrol.audit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Append-only business audit entry. It intentionally has no foreign keys to preserve past facts. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_entry")
public class AuditEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(nullable = false, length = 64, updatable = false)
    private String module;

    @Column(nullable = false, length = 96, updatable = false)
    private String action;

    @Column(name = "subject_type", nullable = false, length = 64, updatable = false)
    private String subjectType;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private UUID subjectId;

    @Column(name = "previous_state", length = 64, updatable = false)
    private String previousState;

    @Column(name = "new_state", length = 64, updatable = false)
    private String newState;

    @Column(length = 1024, updatable = false)
    private String reason;
}
