package at.control.shiftcontrol.shift.assignment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable transition history; assignment state may be reused after a final decision. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shift_assignment_transition")
public class AssignmentTransitionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false, updatable = false)
    private UUID assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 32, updatable = false)
    private AssignmentState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 32, updatable = false)
    private AssignmentState toState;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(length = 1024, updatable = false)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
