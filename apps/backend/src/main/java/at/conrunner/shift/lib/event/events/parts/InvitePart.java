package at.conrunner.shift.lib.event.events.parts;

import java.time.Instant;
import java.util.Collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import at.conrunner.shift.entity.ShiftPlanInvite;
import at.conrunner.shift.lib.type.ShiftPlanInviteType;

@AllArgsConstructor
@Data
public class InvitePart {
    @NotNull
    private String id;

    @NotNull
    private String code;

    @NotNull
    private ShiftPlanInviteType type;

    @NotNull
    private boolean active;

    private Instant expiresAt;
    private Integer maxUses;

    @NotNull
    private int usedCount;

    private Collection<RolePart> autoAssignedRoles;

    @NotNull
    private Instant createdAt;

    private Instant revokedAt;

    @NonNull
    public static InvitePart of(@NonNull ShiftPlanInvite invite) {
        return new InvitePart(
            String.valueOf(invite.getId()),
            invite.getCode(),
            invite.getType(),
            invite.isActive(),
            invite.getExpiresAt(),
            invite.getMaxUses(),
            invite.getUses(),
            invite.getAutoAssignRoles() != null
                ? invite.getAutoAssignRoles().stream()
                    .map(RolePart::of)
                    .toList() : null,
            invite.getCreatedAt(),
            invite.getRevokedAt()
        );
    }
}
