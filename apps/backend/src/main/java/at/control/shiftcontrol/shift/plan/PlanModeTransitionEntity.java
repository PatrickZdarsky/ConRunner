package at.control.shiftcontrol.shift.plan;

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

/** Immutable audit record for the command that changes a plan's default staffing mode. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shift_plan_mode_transition")
public class PlanModeTransitionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_mode", nullable = false, length = 32, updatable = false)
    private StaffingMode oldMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_mode", nullable = false, length = 32, updatable = false)
    private StaffingMode newMode;

    @Column(name = "application_policy", nullable = false, length = 96, updatable = false)
    private String applicationPolicy;

    @Column(name = "resolved_join_request_count", nullable = false, updatable = false)
    private int resolvedJoinRequestCount;

    @Column(name = "resolved_leave_request_count", nullable = false, updatable = false)
    private int resolvedLeaveRequestCount;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
