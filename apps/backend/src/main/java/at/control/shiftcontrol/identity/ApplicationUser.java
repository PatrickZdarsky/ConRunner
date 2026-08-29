package at.control.shiftcontrol.identity;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * This is an abstract representation of an application user for authentication and authorization purposes.
 * It is represented by a JWT token, which contains the necessary information about the user.
 */

@Getter
public abstract class ApplicationUser implements UserDetails {
    private final Collection<? extends GrantedAuthority> authorities;
    private final String username;
    private final boolean enabled;

    protected ApplicationUser(Collection<? extends GrantedAuthority> authorities, String username, boolean enabled) {
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
