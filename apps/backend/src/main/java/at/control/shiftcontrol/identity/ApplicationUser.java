package at.control.shiftcontrol.identity;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * The authenticated application actor. Its identifier is owned by ShiftControl; OIDC claims are used only
 * while resolving the actor during authentication.
 */

@Getter
public abstract class ApplicationUser implements UserDetails {
    private final UUID userId;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String username;
    private final boolean enabled;

    protected ApplicationUser(UUID userId,
                              Collection<? extends GrantedAuthority> authorities,
                              String username,
                              boolean enabled) {
        this.userId = userId;
        this.authorities = authorities;
        this.username = username;
        this.enabled = enabled;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
