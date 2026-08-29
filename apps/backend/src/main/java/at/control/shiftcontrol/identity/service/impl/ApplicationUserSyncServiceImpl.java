package at.control.shiftcontrol.identity.service.impl;

import at.control.shiftcontrol.identity.ApplicationUser;
import at.control.shiftcontrol.identity.ApplicationRole;
import at.control.shiftcontrol.identity.Authorities;
import at.control.shiftcontrol.identity.auth.JwtTokenDecoder;
import at.control.shiftcontrol.identity.entity.ExternalIdentityEntity;
import at.control.shiftcontrol.identity.entity.UserAccountEntity;
import at.control.shiftcontrol.identity.entity.UserAccountStatus;
import at.control.shiftcontrol.identity.repo.ExternalIdentityRepository;
import at.control.shiftcontrol.identity.repo.UserAccountRepository;
import at.control.shiftcontrol.identity.service.ApplicationUserSyncService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read/Write Manager which manages the complete lifecycle of the application users.
 */
@Service
@RequiredArgsConstructor
public class ApplicationUserSyncServiceImpl implements ApplicationUserSyncService {
    private final UserAccountRepository userAccountRepository;
    private final ExternalIdentityRepository externalIdentityRepository;

    /**
     * Constructs a {@link ApplicationUser} from the given JWT token.
     * The user is constructed from the JWT token and the user account is created or updated in the database.
     * @param jwtToken The JWT token to construct the user from.
     * @return The constructed user.
     */
    @Override
    @Transactional
    public @NonNull ApplicationUser constructUserForLogin(@NonNull JwtTokenDecoder.DecodedJwtToken jwtToken) {
        requireIdentityClaim(jwtToken.issuer(), "issuer");
        requireIdentityClaim(jwtToken.subject(), "subject");

        UserAccountEntity userAccount = externalIdentityRepository
            .findByIssuerAndSubject(jwtToken.issuer(), jwtToken.subject())
            .map(ExternalIdentityEntity::getUserAccount)
            .orElseGet(() -> createUserAccount(jwtToken));

        if (updateProfileClaims(userAccount, jwtToken)) {
            userAccount.setUpdatedAt(Instant.now());
            userAccount = userAccountRepository.save(userAccount);
        }

        return toApplicationUser(userAccount, jwtToken.subject());
    }

    //Todo: Add support for matching the same user from multiple externalidentities
    private UserAccountEntity createUserAccount(JwtTokenDecoder.DecodedJwtToken jwtToken) {
        Instant now = Instant.now();
        String username = firstNonBlank(jwtToken.preferredUsername(), jwtToken.subject());

        UserAccountEntity userAccount = UserAccountEntity.builder()
            .status(UserAccountStatus.ACTIVE)
            .preferredUsername(username)
            .displayName(username)
            .email(blankToNull(jwtToken.email()))
            .avatarUrl(blankToNull(jwtToken.avatarUrl()))
            .createdAt(now)
            .updatedAt(now)
            .build();

        userAccount.addExternalIdentity(ExternalIdentityEntity.builder()
            .issuer(jwtToken.issuer())
            .subject(jwtToken.subject())
            .createdAt(now)
            .build());

        return userAccountRepository.save(userAccount);
    }

    private boolean updateProfileClaims(UserAccountEntity userAccount,
                                        JwtTokenDecoder.DecodedJwtToken jwtToken) {
        boolean changed = false;
        changed |= setIfPresentAndChanged(
            jwtToken.preferredUsername(), userAccount.getPreferredUsername(), userAccount::setPreferredUsername);
        changed |= setIfPresentAndChanged(jwtToken.email(), userAccount.getEmail(), userAccount::setEmail);
        changed |= setIfPresentAndChanged(jwtToken.avatarUrl(), userAccount.getAvatarUrl(), userAccount::setAvatarUrl);
        return changed;
    }

    private ApplicationUser toApplicationUser(UserAccountEntity userAccount, String subject) {
        List<GrantedAuthority> authorities = userAccount.getApplicationRoles() == null
            ? List.of()
            : userAccount.getApplicationRoles().stream()
                .map(this::toAuthority)
                .toList();

        return new SyncedApplicationUser(
            requireUserId(userAccount),
            authorities,
            firstNonBlank(userAccount.getPreferredUsername(), subject),
            userAccount.getStatus() == UserAccountStatus.ACTIVE);
    }

    private GrantedAuthority toAuthority(ApplicationRole role) {
        return switch (role) {
            case PLATFORM_ADMIN -> new SimpleGrantedAuthority(Authorities.PLATFORM_ADMIN_ROLE);
        };
    }

    private static boolean setIfPresentAndChanged(String candidate,
                                                  String currentValue,
                                                  java.util.function.Consumer<String> setter) {
        String value = blankToNull(candidate);
        if (value == null || Objects.equals(value, currentValue)) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private static String firstNonBlank(String first, String fallback) {
        String value = blankToNull(first);
        return value != null ? value : fallback;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void requireIdentityClaim(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT " + claimName + " claim must not be blank");
        }
    }

    private static UUID requireUserId(UserAccountEntity userAccount) {
        if (userAccount.getId() == null) {
            throw new IllegalStateException("Persisted user account has no ID");
        }
        return userAccount.getId();
    }

    private static final class SyncedApplicationUser extends ApplicationUser {
        private SyncedApplicationUser(UUID userId,
                                      List<GrantedAuthority> authorities,
                                      String username,
                                      boolean enabled) {
            super(userId, authorities, username, enabled);
        }
    }
}
