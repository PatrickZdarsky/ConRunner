package at.control.shiftcontrol.identity;

import at.control.shiftcontrol.identity.entity.UserAccountEntity;

import java.util.List;
import java.util.UUID;

/**
 * Stable user information exposed to the rest of the application without leaking the persistence entity.
 */
public record UserInfo(
    UUID id,
    String username,
    String email,
    String avatarUrl,
    List<ApplicationRole> applicationRoles
) {
    public static UserInfo from(UserAccountEntity account) {
        return new UserInfo(
            account.getId(),
            firstNonBlank(account.getUsername(), account.getId().toString()),
            blankToEmpty(account.getEmail()),
            account.getAvatarUrl(),
            account.getApplicationRoles() == null ? List.of() : List.copyOf(account.getApplicationRoles())
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
