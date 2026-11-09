package at.control.shiftcontrol.shift.auth;

import java.io.IOException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.auth.ApplicationUser;
import at.shiftcontrol.shiftservice.auth.user.ServiceUser;
import at.control.shiftcontrol.shift.userdirectory.current.CurrentUserProfileSyncService;
@Deprecated
@RequiredArgsConstructor
public class CurrentUserProfileSyncFilter extends OncePerRequestFilter {
    private final CurrentUserProfileSyncService currentUserProfileSyncService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.getPrincipal() instanceof ApplicationUser
            && !(authentication.getPrincipal() instanceof ServiceUser)
            && authentication.getDetails() instanceof Jwt) {
            currentUserProfileSyncService.syncCurrentSubjectIfStale();
        }

        filterChain.doFilter(request, response);
    }
}
