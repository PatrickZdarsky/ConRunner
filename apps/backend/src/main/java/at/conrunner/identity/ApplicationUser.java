package at.conrunner.identity;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The authenticated application actor. Its identifier is owned by ShiftControl; OIDC claims are used only
 * while resolving the actor during authentication.
 */

@Getter
public abstract class ApplicationUser implements UserDetails, AuthenticatedActor {
    private final UUID userId;
    private final Collection<GrantedAuthority> authorities;
    private final String username;
    private final boolean enabled;

    protected ApplicationUser(UUID userId,
                              Collection<? extends GrantedAuthority> authorities,
                              String username,
                              boolean enabled) {
        this.userId = userId;
        this.authorities = List.copyOf(authorities);
        this.username = username;
        this.enabled = enabled;
    }

    @Override
    public ActorType type() {
        return ActorType.HUMAN;
    }

    @Override
    public String name() {
        return username;
    }

    @Override
    public Optional<UUID> userId() {
        return Optional.of(userId);
    }

    @Override
    public Collection<GrantedAuthority> authorities() {
        return authorities;
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
