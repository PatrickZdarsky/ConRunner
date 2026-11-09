package at.control.shiftcontrol.identity.auth;

import at.control.shiftcontrol.identity.AuthenticatedActor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;

@EqualsAndHashCode(callSuper = true)
@Getter
public class UserAuthenticationToken extends AbstractAuthenticationToken {
    private final AuthenticatedActor principal;
    private final String credentials;

    public UserAuthenticationToken(@NonNull AuthenticatedActor principal, String credentials) {
        super(principal.authorities());
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);  // must use super, as we override
    }

    @Override
    public String getName() {
        return principal.name();
    }
}
