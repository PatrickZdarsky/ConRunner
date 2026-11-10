package at.control.shiftcontrol.shift.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@TestConfiguration
@EnableMethodSecurity
@Profile("test")
public class TestSecurityConfig {
    public static final String HDR_TYPE = "X-Test-UserType";
    public static final String HDR_USERID = "X-Test-UserId";
    public static final String HDR_USERNAME = "X-Test-Username";

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http, TestAuthFilter testAuthFilter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .securityContext(sc -> sc.requireExplicitSave(false))
            .addFilterBefore(testAuthFilter, AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }

    @Bean
    public TestAuthFilter testAuthFilter() {
        return new TestAuthFilter();
    }
}
