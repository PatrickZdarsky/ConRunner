package at.control.shiftcontrol;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "oidc.provider.issuer-uri=https://identity.example.test/realms/shiftcontrol",
    "oidc.provider.jwk-set-uri=https://identity.example.test/realms/shiftcontrol/protocol/openid-connect/certs",
    "oidc.provider.audience=shiftcontrol-backend"
})
class ConRunnerApplicationTests {

	@Test
	void contextLoads() {
	}

}
