package at.control.shiftcontrol.identity.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenDecoderTest {
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
