package at.conrunner.shift.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(@Value("${shiftcontrol.http.cors.allowed-origins:}") String allowedOrigins) {
        var origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::strip)
            .filter(origin -> !origin.isEmpty())
            .toList();
        if (origins.stream().anyMatch(origin -> origin.contains("*") || origin.equals("null"))) {
            throw new IllegalArgumentException("Configure explicit origins for cross-origin API requests");
        }

        var config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        var registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
