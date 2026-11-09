package at.control.shiftcontrol.shift.auth;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.lib.auth.ApplicationUser;
import at.shiftcontrol.lib.exception.IllegalArgumentException;
import at.shiftcontrol.shiftservice.auth.config.props.OidcProviderProps;
import at.shiftcontrol.shiftservice.auth.user.ShiftControlUser;
@Deprecated
@Slf4j
@RequiredArgsConstructor
@Component
public class ApplicationUserManager {
    private Cache<String, ShiftControlUser> userCache;
    private final UserAttributeProvider userAttributeProvider;
    private final OidcProviderProps oidcProviderProps;

    @PostConstruct
    public void init() {
        userCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterAccess(Duration.ofMinutes(20))
            .build();
    }

    public ApplicationUser getOrCreateUser(Jwt jwt) {
        var userId = jwt.getClaimAsString("sub");
        if (userId == null) {
            throw new IllegalArgumentException("User token does not contain 'sub' claim");
        }

        Set<String> scopeAuthorities = extractAuthorities(jwt);
        String username = firstNonBlank(jwt.getClaimAsString(oidcProviderProps.usernameClaim()), jwt.getClaimAsString("client_id"), userId);

        if (isMachineToken(jwt, scopeAuthorities)) {
            log.debug("Creating service ApplicationUser for username {} and userId {}", username, userId);
            return getOrCreateServiceUser(userId, username, scopeAuthorities);
        }

        boolean isPlatformAdmin = hasPlatformAdminAuthority(scopeAuthorities);
        log.debug("Creating human ApplicationUser for username {} and userId {} (platformAdmin={})", username, userId, isPlatformAdmin);
        return getOrCreateHumanUser(userId, isPlatformAdmin, username, scopeAuthorities);
    }

    private ShiftControlUser getOrCreateHumanUser(String userId, boolean isPlatformAdmin, String username, Set<String> scopeAuthorities) {
        String cacheKey = buildHumanUserCacheKey(userId, isPlatformAdmin, username, scopeAuthorities);
        var user = userCache.getIfPresent(cacheKey);
        if (user == null) {
            user = ApplicationUserFactory.createHumanUser(username, userId, scopeAuthorities, userAttributeProvider, isPlatformAdmin);
            userCache.put(cacheKey, user);
        }

        return user;
    }

    private ShiftControlUser getOrCreateServiceUser(String userId, String username, Set<String> scopeAuthorities) {
        String cacheKey = "service:" + userId + ":" + String.join(",", scopeAuthorities);
        var user = userCache.getIfPresent(cacheKey);
        if (user == null) {
            user = ApplicationUserFactory.createServiceUser(username, userId, scopeAuthorities);
            userCache.put(cacheKey, user);
        }

        return user;
    }

    private Set<String> extractAuthorities(Jwt jwt) {
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        addClaimValues(authorities, jwt.getClaimAsString("scope"));
        addClaimValues(authorities, jwt.getClaims().get("scp"));
        addClaimValues(authorities, jwt.getClaims().get("roles"));
        addNestedRoleValues(authorities, jwt.getClaims().get("realm_access"));
        addResourceAccessRoles(authorities, jwt.getClaims().get("resource_access"));

        return authorities;
    }

    private void addClaimValues(LinkedHashSet<String> authorities, Object claimValue) {
        if (claimValue instanceof Collection<?> values) {
            values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .forEach(authorities::add);
            return;
        }

        if (claimValue instanceof String stringValue && !stringValue.isBlank()) {
            Arrays.stream(stringValue.split("\\s+"))
                .filter(value -> !value.isBlank())
                .forEach(authorities::add);
        }
    }

    @SuppressWarnings("unchecked")
    private void addNestedRoleValues(LinkedHashSet<String> authorities, Object accessClaim) {
        if (accessClaim instanceof Map<?, ?> accessMap) {
            addClaimValues(authorities, accessMap.get("roles"));
        }
    }

    @SuppressWarnings("unchecked")
    private void addResourceAccessRoles(LinkedHashSet<String> authorities, Object resourceAccessClaim) {
        if (!(resourceAccessClaim instanceof Map<?, ?> resourceAccessMap)) {
            return;
        }

        resourceAccessMap.values().stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .forEach(accessEntry -> addClaimValues(authorities, accessEntry.get("roles")));
    }

    private boolean hasPlatformAdminAuthority(Set<String> authorities) {
        return authorities.stream().anyMatch(this::isPlatformAdminAuthority);
    }

    private boolean isPlatformAdminAuthority(String authority) {
        return "ADMIN".equalsIgnoreCase(authority)
            || Authorities.PLATFORM_ADMIN_ROLE.equalsIgnoreCase(authority)
            || Authorities.LEGACY_PLATFORM_ADMIN_SCOPE.equals(authority);
    }

    private String buildHumanUserCacheKey(String userId, boolean isPlatformAdmin, String username, Set<String> scopeAuthorities) {
        String authorityKey = scopeAuthorities.stream()
            .sorted()
            .collect(Collectors.joining(","));
        return "human:" + userId + ":" + isPlatformAdmin + ":" + firstNonBlank(username, "") + ":" + authorityKey;
    }

    private boolean isMachineToken(Jwt jwt, Set<String> scopeAuthorities) {
        if (scopeAuthorities.contains("openid")) {
            return false;
        }

        return Stream.of(
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("userType")
            )
            .allMatch(value -> value == null || value.isBlank());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }
}
