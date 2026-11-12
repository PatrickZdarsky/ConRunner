package at.conrunner.shift.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;

class CorsConfigTest {
    private static final String WEB_ORIGIN = "https://web.example.test";
    private final CorsConfig config = new CorsConfig();

    @Test
    void allowsPreflightFromAConfiguredOrigin() throws Exception {
        var response = preflight("https://other.example.test, " + WEB_ORIGIN, WEB_ORIGIN);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(WEB_ORIGIN);
    }

    @Test
    void deniesUnconfiguredCrossOriginRequests() throws Exception {
        var response = preflight("", WEB_ORIGIN);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    void rejectsWildcardOrigins() {
        assertThatIllegalArgumentException().isThrownBy(() -> config.corsFilter("*/"));
    }

    private MockHttpServletResponse preflight(String allowedOrigins, String origin) throws Exception {
        var request = new MockHttpServletRequest("OPTIONS", "/api/v1/events");
        request.addHeader(ORIGIN, origin);
        request.addHeader(ACCESS_CONTROL_REQUEST_METHOD, "POST");
        var response = new MockHttpServletResponse();
        config.corsFilter(allowedOrigins).getFilter().doFilter(request, response, new MockFilterChain());
        return response;
    }
}
