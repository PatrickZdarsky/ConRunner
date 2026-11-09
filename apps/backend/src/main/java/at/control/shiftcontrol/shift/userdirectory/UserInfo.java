package at.control.shiftcontrol.shift.userdirectory;

import at.control.shiftcontrol.shift.entity.UserAccount;

/**
 * Stable user information exposed to the rest of the application without leaking the persistence entity.
 */
public record UserInfo(
    String id,
    String username,
    String firstName,
    String lastName,
    String email,
    String profile,
    boolean isPlatformAdmin
) {
    public static UserInfo from(String userId, UserAccount account) {
        return new UserInfo(
            userId,
            firstNonBlank(account.getPreferredUsername(), userId),
            blankToEmpty(account.getFirstName()),
            blankToEmpty(account.getLastName()),
            blankToEmpty(account.getEmail()),
            account.getProfile(),
            account.isPlatformAdmin()
        );
    }

    public static UserInfo fallback(String userId) {
        return new UserInfo(userId, userId, "", "", "", null, false);
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
