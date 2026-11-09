package at.control.shiftcontrol.shift.auth.user;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import at.shiftcontrol.shiftservice.auth.UserAttributeProvider;

public class HumanUser extends ShiftUser {
    private final UserAttributeProvider attributeProvider;
    private final boolean platformAdmin;

    public HumanUser(
        Collection<? extends GrantedAuthority> authorities,
        String username,
        String userId,
        UserAttributeProvider attributeProvider,
        boolean platformAdmin
    ) {
        super(authorities, username, userId);
        this.attributeProvider = attributeProvider;
        this.platformAdmin = platformAdmin;
    }

    @Override
    public boolean isVolunteerInPlan(long shiftPlanId) {
        return getRelevantVolunteerPlans().contains(shiftPlanId);
    }

    @Override
    public boolean isPlannerInPlan(long shiftPlanId) {
        return getRelevantPlannerPlans().contains(shiftPlanId);
    }

    @Override
    public boolean isLockedInPlan(long shiftPlanId) {
        return getLockedPlans().contains(shiftPlanId);
    }

    @Override
    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public Set<Long> getRelevantVolunteerPlans() {
        return attributeProvider.getPlansWhereUserIsVolunteer(getUserId());
    }

    public Set<Long> getRelevantPlannerPlans() {
        return attributeProvider.getPlansWhereUserIsPlanner(getUserId());
    }

    public Set<Long> getLockedPlans() {
        return attributeProvider.getPlansWhereUserIsLocked(getUserId());
    }
}
