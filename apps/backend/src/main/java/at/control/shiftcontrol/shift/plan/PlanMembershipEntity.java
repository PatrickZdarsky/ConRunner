package at.control.shiftcontrol.shift.plan;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A membership is always volunteer access; a planner is additionally a volunteer. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shift_plan_membership")
public class PlanMembershipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private boolean planner;

    @Column(name = "volunteer_changes_restricted", nullable = false)
    private boolean volunteerChangesRestricted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "shift_plan_membership_role", joinColumns = @JoinColumn(name = "membership_id"))
    @Column(name = "role_id", nullable = false)
    private Set<UUID> roleIds = new LinkedHashSet<>();

    @Version
    @Column(nullable = false)
    private long version;

    public PlanMembershipEntity(UUID planId, UUID userId, boolean planner, Instant createdAt) {
        this.planId = planId;
        this.userId = userId;
        this.planner = planner;
        this.createdAt = createdAt;
    }

    public boolean grantPlanner() {
        if (planner) {
            return false;
        }
        planner = true;
        return true;
    }
}
