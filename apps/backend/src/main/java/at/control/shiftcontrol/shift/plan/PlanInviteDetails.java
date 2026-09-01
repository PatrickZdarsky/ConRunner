package at.control.shiftcontrol.shift.plan;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Planner-authorized management view. The reversible code is intentional so planners can redisplay its invite link. */
public record PlanInviteDetails(UUID id, UUID planId, String code, PlanInviteType type, boolean active, Instant expiresAt,
                                Integer useLimit, int uses, Instant createdAt, Instant revokedAt, Set<UUID> roleIds) {
}
