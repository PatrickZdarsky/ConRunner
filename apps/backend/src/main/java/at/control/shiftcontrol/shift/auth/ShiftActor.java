package at.control.shiftcontrol.shift.auth;

import at.control.shiftcontrol.identity.AuthenticatedActor;
import at.control.shiftcontrol.identity.Authorities;
import at.control.shiftcontrol.platform.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;

import lombok.Getter;


import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ShiftActor {
    private final AuthenticatedActor actor;
    private final UserAttributeProvider attributeProvider;

    public boolean isMachine() {
        return actor.isMachine();
    }

    public Optional<UUID> getUserId() {
        return actor.userId();
    }

    public UUID requireUserId() {
        return getUserId().orElseThrow(() -> new UnauthorizedException("A human user is required"));
    }

    public String getName() {
        return actor.name();
    }

    public boolean isVolunteerInPlan(long shiftPlanId) {
        return getRelevantVolunteerPlans().contains(shiftPlanId);
    }

    public boolean isPlannerInPlan(long shiftPlanId) {
        return getRelevantPlannerPlans().contains(shiftPlanId);
    }

    public boolean isLockedInPlan(long shiftPlanId) {
        return getLockedPlans().contains(shiftPlanId);
    }

    public boolean hasAuthority(String authority) {
        return actor.authorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    public boolean isPlatformAdmin() {
        return actor.authorities().stream().anyMatch(authority -> authority.getAuthority().equalsIgnoreCase(Authorities.PLATFORM_ADMIN_ROLE));
    }

    public Set<Long> getRelevantVolunteerPlans() {
        if (actor.userId().isEmpty()) {
            return Collections.emptySet();
        }
        return attributeProvider.getPlansWhereUserIsVolunteer(actor.userId().get());
    }

    public Set<Long> getRelevantPlannerPlans() {
        if (actor.userId().isEmpty()) {
            return Collections.emptySet();
        }
        return attributeProvider.getPlansWhereUserIsPlanner(actor.userId().get());
    }

    public Set<Long> getLockedPlans() {
        if (actor.userId().isEmpty()) {
            return Collections.emptySet();
        }
        return attributeProvider.getPlansWhereUserIsLocked(actor.userId().get());
    }
}
