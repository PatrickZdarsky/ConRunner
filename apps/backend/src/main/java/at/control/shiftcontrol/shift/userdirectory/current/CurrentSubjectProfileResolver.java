package at.control.shiftcontrol.shift.userdirectory.current;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.lib.exception.IllegalArgumentException;
import at.control.shiftcontrol.shift.auth.ApplicationUserProvider;
import at.control.shiftcontrol.identity.config.props.OidcProviderProps;

@Component
@RequiredArgsConstructor
public class CurrentSubjectProfileResolver {
    private final ApplicationUserProvider applicationUserProvider;
    private final OidcProviderProps oidcProviderProps;

    public CurrentSubjectProfile resolveCurrentSubject() {
        Jwt jwt = applicationUserProvider.getCurrentJwt();
        String subject = jwt.getClaimAsString("sub");
        if (subject == null) {
            throw new IllegalArgumentException("User token does not contain 'sub' claim");
        }
        if (jwt.getIssuer() == null) {
            throw new IllegalArgumentException("User token does not contain 'iss' claim");
        }

        return new CurrentSubjectProfile(
            jwt.getIssuer().toString(),
            subject,
            firstNonBlank(jwt.getClaimAsString(oidcProviderProps.usernameClaim()), applicationUserProvider.getCurrentUser().getUsername()),
            jwt.getClaimAsString("given_name"),
            jwt.getClaimAsString("family_name"),
            jwt.getClaimAsString("email"),
            firstNonBlank(jwt.getClaimAsString("picture"), jwt.getClaimAsString("profile")),
            jwt.getClaims().containsKey("email_verified") ? jwt.getClaimAsBoolean("email_verified") : null,
            applicationUserProvider.currentUserHasAuthority("ADMIN")
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }
}
