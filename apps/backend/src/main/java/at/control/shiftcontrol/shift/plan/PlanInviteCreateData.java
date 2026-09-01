package at.control.shiftcontrol.shift.plan;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import at.control.shiftcontrol.shift.error.ShiftValidationException;

public record PlanInviteCreateData(PlanInviteType type, Instant expiresAt, Integer useLimit, Set<java.util.UUID> roleIds) {
    public PlanInviteCreateData {
        if (type == null) {
            throw new ShiftValidationException("Invite type is required");
        }
        if (useLimit != null && useLimit <= 0) {
            throw new ShiftValidationException("Invite use limit must be positive");
        }
        roleIds = roleIds == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(roleIds));
    }
}
