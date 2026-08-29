package at.control.shiftcontrol.identity.auth;

import at.control.shiftcontrol.identity.ApplicationUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@EqualsAndHashCode(callSuper = true)
@Getter
public class UserAuthenticationToken extends AbstractAuthenticationToken {
    private final ApplicationUser principal;
    private final String credentials;

    public UserAuthenticationToken(@NonNull ApplicationUser principal,
                                   String credentials,
                                   Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);  // must use super, as we override
    }
}
