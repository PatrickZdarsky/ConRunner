package at.control.shiftcontrol.identity;

import at.control.shiftcontrol.identity.entity.UserAccountEntity;

import java.util.Collections;
import java.util.List;

/**
 * Stable user information exposed to the rest of the application without leaking the persistence entity.
 */
public record UserInfo(
    String id,
    String username,
    String firstName,
    String lastName,
    String email,
    String avatarUrl,
    List<ApplicationRole> applicationRoles
) {
    public static UserInfo from(String userId, UserAccountEntity account) {
        return new UserInfo(
            userId,
            firstNonBlank(account.getPreferredUsername(), userId),
            blankToEmpty(account.getFirstName()),
            blankToEmpty(account.getLastName()),
            blankToEmpty(account.getEmail()),
            account.getAvatarUrl(),
            account.getApplicationRoles()
        );
    }

    public static UserInfo fallback(String userId) {
        return new UserInfo(userId, userId, "", "", "", null, Collections.emptyList());
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
