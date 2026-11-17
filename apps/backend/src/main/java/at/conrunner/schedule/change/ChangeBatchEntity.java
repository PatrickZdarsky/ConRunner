package at.conrunner.schedule.change;

import java.util.UUID;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "schedule_change_batch")
public class ChangeBatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChangeState state = ChangeState.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ChangeOrigin origin = ChangeOrigin.MANUAL;

    @Column(name = "sequence")
    private Long sequence;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "last_edited_by")
    private UUID lastEditedBy;

    @Column(name = "committed_by")
    private UUID committedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "committed_at")
    private Instant committedAt;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private long version;
}
