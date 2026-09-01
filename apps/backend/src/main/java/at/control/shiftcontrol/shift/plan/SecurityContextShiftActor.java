package at.control.shiftcontrol.shift.plan;

import at.control.shiftcontrol.identity.ApplicationUser;
import at.control.shiftcontrol.identity.Authorities;
import at.control.shiftcontrol.shift.error.ShiftAccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class SecurityContextShiftActor implements CurrentShiftActor {
    @Override
    public ShiftActor current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof ApplicationUser user)) {
            throw new ShiftAccessDeniedException("An authenticated application user is required");
        }
        boolean platformAdmin = user.getAuthorities().stream()
            .anyMatch(authority -> Authorities.PLATFORM_ADMIN_ROLE.equals(authority.getAuthority()));
        return new ShiftActor(user.getUserId(), platformAdmin);
    }
}
