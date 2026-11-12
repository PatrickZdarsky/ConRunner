package at.conrunner.identity.auth;

import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URL;
import java.util.Optional;

public class JwtTokenDecoder {
    public static DecodedJwtToken decode(Jwt token, String usernameClaim) {
        String issuer = Optional.ofNullable(token.getIssuer()).map(URL::toString).orElse("");
        String subject = token.getSubject();
        String username = token.getClaimAsString(usernameClaim);
        String email = token.getClaimAsString("email");
        String avatarUrl = firstNonBlank(token.getClaimAsString("avatar_url"), token.getClaimAsString("picture"), token.getClaimAsString("profile"));

        return new DecodedJwtToken(issuer, subject, username, email, avatarUrl,
            Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified")));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    public record DecodedJwtToken(String issuer,
                                  String subject,
                                  String username,
                                  String email,
                                  String avatarUrl,
                                  boolean emailVerified) {
        public DecodedJwtToken(String issuer, String subject, String username, String email, String avatarUrl) {
            this(issuer, subject, username, email, avatarUrl, false);
        }
    }
}
