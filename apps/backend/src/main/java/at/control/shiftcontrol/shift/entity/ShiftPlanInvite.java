package at.control.shiftcontrol.shift.entity;

import java.time.Instant;
import java.util.Collection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.shiftcontrol.lib.type.ShiftPlanInviteType;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "shift_plan_invite")
public class ShiftPlanInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private ShiftPlanInviteType type;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_plan_id", nullable = false)
    private ShiftPlan shiftPlan;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "max_uses")
    private Integer maxUses; // null = unlimited

    @NotNull
    @Column(name = "uses", nullable = false)
    private int uses = 0;

    @ManyToMany
    @JoinTable(
        name = "shift_plan_invite_role",
        joinColumns = @JoinColumn(name = "invite_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    // add roles which get assigned when using this invite
    private Collection<Role> autoAssignRoles;

    // optional auditing
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
