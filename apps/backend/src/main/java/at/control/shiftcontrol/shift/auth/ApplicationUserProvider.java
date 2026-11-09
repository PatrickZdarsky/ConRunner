package at.control.shiftcontrol.shift.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import at.shiftcontrol.lib.auth.impl.UserProviderImpl;
import at.control.shiftcontrol.shift.lib.exception.UnauthorizedException;
import at.shiftcontrol.shiftservice.auth.user.ShiftControlUser;
@Deprecated
@Component
public class ApplicationUserProvider extends UserProviderImpl {
    public <T extends ShiftControlUser> T getCurrentUser() {
        return (T) getApplicationUser();
    }

    public boolean currentUserHasAuthority(String authority) {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    public Jwt getCurrentJwt() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UnauthorizedException("No authentication found");
        }

        if (!(authentication.getDetails() instanceof Jwt jwt)) {
            throw new UnauthorizedException("Authentication does not expose a decoded JWT");
        }

        return jwt;
    }
}
