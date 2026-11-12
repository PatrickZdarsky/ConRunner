package at.conrunner.shift.config;

import at.conrunner.identity.ActorType;
import at.conrunner.identity.AuthenticatedActor;
import at.conrunner.identity.Authorities;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String type = request.getHeader(TestSecurityConfig.HDR_TYPE);
        if (type != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String id = Optional.ofNullable(request.getHeader(TestSecurityConfig.HDR_USERID))
                .orElse("00000000-0000-0000-0000-000000000001");
            String name = Optional.ofNullable(request.getHeader(TestSecurityConfig.HDR_USERNAME)).orElse("test");
            List<GrantedAuthority> authorities = "ADMIN".equalsIgnoreCase(type)
                ? List.of(new SimpleGrantedAuthority(Authorities.PLATFORM_ADMIN_ROLE)) : List.of();
            AuthenticatedActor actor = new AuthenticatedActor() {
                public ActorType type() { return ActorType.HUMAN; }
                public String name() { return name; }
                public Optional<UUID> userId() { return Optional.of(UUID.fromString(id)); }
                public Collection<? extends GrantedAuthority> authorities() { return authorities; }
            };
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "N/A", authorities));
        }
        try { chain.doFilter(request, response); }
        finally { SecurityContextHolder.clearContext(); }
    }
}
