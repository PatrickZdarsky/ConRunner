package at.control.shiftcontrol.shift.auth.user;

import java.util.Collection;

import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.shift.auth.UserAttributeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import lombok.Getter;

import at.shiftcontrol.lib.auth.ApplicationUser;

@Getter
@RequiredArgsConstructor
public class ShiftUser {
    private final Authentication authentication;
    private final UserAttributeProvider attributeProvider;

    public boolean isMachine() {
        return authentication.getPrincipal()
            instanceof MachineClientAuthorizer.MachinePrincipal;
    }

    public Optional<UUID> userId() {
        return authentication.getPrincipal() instanceof ApplicationUser user
            ? Optional.of(user.getUserId())
            : Optional.empty();
    }

    public boolean isVolunteerInPlan(long shiftPlanId);

    public boolean isPlannerInPlan(long shiftPlanId);

    public boolean isLockedInPlan(long shiftPlanId);

    public boolean isPlatformAdmin() {
        return false;
    }
}
