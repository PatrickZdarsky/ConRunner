package at.control.shiftcontrol.identity.auth;

import at.control.shiftcontrol.identity.config.props.OidcProviderProps;
import at.control.shiftcontrol.identity.service.ApplicationUserSyncService;
import at.control.shiftcontrol.platform.NonDevTestCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Conditional(NonDevTestCondition.class)
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@Configuration
@RequiredArgsConstructor
public class JwtSecurityConfig {
    private final ApplicationUserSyncService applicationUserSyncService;

    /**
     * Main authentication entry point for the application.
     * @return {@link Converter} which converts the JWT to a {@link UserAuthenticationToken}
     */
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            var decodedToken = JwtTokenDecoder.decode(jwt);
            var applicationUser = applicationUserSyncService.constructUserForLogin(decodedToken);
            if (!applicationUser.isEnabled()) {
                throw new DisabledException("User account is suspended");
            }
            var token = new UserAuthenticationToken(applicationUser, jwt.getTokenValue(), applicationUser.getAuthorities());
            token.setDetails(jwt);
            return token;
        };
    }

    @Bean
    public JwtDecoder jwtDecoder(OidcProviderProps oidcProviderProps) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(oidcProviderProps.jwkSetUri()).build();
        decoder.setJwtValidator(jwtValidator(oidcProviderProps));
        return decoder;
    }

    OAuth2TokenValidator<Jwt> jwtValidator(OidcProviderProps oidcProviderProps) {
        OAuth2TokenValidator<Jwt> issuerValidator = token -> {
            String issuer = token.getIssuer() == null ? null : token.getIssuer().toString();
            if (issuer == null || !oidcProviderProps.allowedIssuers().contains(issuer)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "The iss claim is not valid: " + token.getIssuer(),
                    null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        OAuth2TokenValidator<Jwt> audienceValidator = token -> token.getAudience().contains(oidcProviderProps.audience())
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "The aud claim does not contain the expected audience",
                null));

        return JwtValidators.createDefaultWithValidators(issuerValidator, audienceValidator);
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/swagger-ui/**")
                .permitAll()
                .requestMatchers("/swagger")
                .permitAll()
                .requestMatchers("/v3/api-docs*/**")
                .permitAll()
                .requestMatchers("/api/v1/calendar/share/**")
                .permitAll()
                .requestMatchers("/api/v1/reward-points/share/**") // permit access to reward points share endpoint via token
                .permitAll()
                .requestMatchers("/api/v1/invites/{inviteCode}")
                .permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                .permitAll() // Permit all OPTIONS requests (preflight))
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
