package at.conrunner.identity.auth;

import at.conrunner.identity.ActorType;
import at.conrunner.identity.config.props.MachineClientRegistration;
import at.conrunner.identity.AuthenticatedActor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MachineClientAuthorizer {
    private final Map<String, Set<String>> authoritiesByClientId;

    public MachineClientAuthorizer(Collection<MachineClientRegistration> machineClients) {
        this.authoritiesByClientId = machineClients.stream().collect(Collectors.toUnmodifiableMap(
            MachineClientRegistration::clientId,
            MachineClientRegistration::authorities,
            (first, second) -> {
                throw new IllegalArgumentException("A machine client may be configured only once");
            }
        ));
    }

    public Optional<AuthenticatedActor> authenticate(Jwt jwt) {
        String clientId = firstNonBlank(jwt.getClaimAsString("client_id"), jwt.getClaimAsString("azp"));
        if (clientId == null || !authoritiesByClientId.containsKey(clientId)) {
            return Optional.empty();
        }

        Set<String> requiredAuthorities = authoritiesByClientId.get(clientId);
        Set<String> tokenScopes = extractScopes(jwt);
        if (!tokenScopes.containsAll(requiredAuthorities)) {
            throw new BadCredentialsException("Machine token is missing a configured authority");
        }

        List<GrantedAuthority> authorities = requiredAuthorities.stream()
            .sorted()
            .map(SimpleGrantedAuthority::new)
            .map(GrantedAuthority.class::cast)
            .toList();
        return Optional.of(new MachinePrincipal(clientId, authorities));
    }

    private Set<String> extractScopes(Jwt jwt) {
        Set<String> scopes = new LinkedHashSet<>();
        addScopeValues(scopes, jwt.getClaimAsString("scope"));
        addScopeValues(scopes, jwt.getClaims().get("scp"));
        return scopes;
    }

    private void addScopeValues(Set<String> scopes, Object value) {
        if (value instanceof String stringValue) {
            Arrays.stream(stringValue.split("\\s+"))
                .filter(scope -> !scope.isBlank())
                .forEach(scopes::add);
        } else if (value instanceof Collection<?> values) {
            values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(scope -> !scope.isBlank())
                .forEach(scopes::add);
        }
    }

    private String firstNonBlank(String... values) {
        return Arrays.stream(values)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse(null);
    }

    public record MachinePrincipal(String clientId, List<GrantedAuthority> authorities) implements AuthenticatedActor {
        public MachinePrincipal {
            authorities = List.copyOf(authorities);
        }

        @Override
        public ActorType type() {
            return ActorType.MACHINE;
        }

        @Override
        public String name() {
            return clientId;
        }

        @Override
        public Optional<UUID> userId() {
            return Optional.empty();
        }
    }
}
