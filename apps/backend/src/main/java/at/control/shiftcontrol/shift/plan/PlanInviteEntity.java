package at.control.shiftcontrol.shift.plan;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shift_plan_invite")
public class PlanInviteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    /** High-entropy bearer code retained so an authorized planner can redisplay the invite. */
    @Column(name = "raw_code", nullable = false, unique = true, length = 64, updatable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, updatable = false)
    private PlanInviteType type;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "use_limit")
    private Integer useLimit;

    @Column(name = "uses", nullable = false)
    private int uses;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ElementCollection
    @CollectionTable(name = "shift_plan_invite_role", joinColumns = @JoinColumn(name = "invite_id"))
    @Column(name = "role_id", nullable = false)
    private Set<UUID> roleIds = new LinkedHashSet<>();

    @Version
    @Column(nullable = false)
    private long version;
}
