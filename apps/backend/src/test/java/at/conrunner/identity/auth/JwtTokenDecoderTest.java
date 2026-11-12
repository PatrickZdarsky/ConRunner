package at.conrunner.identity.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JwtTokenDecoderTest {
    @Test
    void emailVerificationRequiresAnExplicitTrueClaim() {
        Jwt.Builder base = Jwt.withTokenValue("token").header("alg", "RS256")
            .issuer("https://identity.example").subject("subject");
        assertFalse(JwtTokenDecoder.decode(base.build(), "preferred_username").emailVerified());
        assertTrue(JwtTokenDecoder.decode(base.claim("email_verified", true).build(), "preferred_username").emailVerified());
    }
    @Test
    void usesTheConfiguredClaimForTheLocalUsername() {
        Jwt token = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer("https://identity.example")
            .subject("subject-1")
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("preferred_username", "ignored-default")
            .claim("nickname", "pat")
            .build();

        var decoded = JwtTokenDecoder.decode(token, "nickname");

        assertEquals("pat", decoded.username());
    }
}
