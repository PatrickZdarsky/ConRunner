package at.control.shiftcontrol.identity;

import at.control.shiftcontrol.identity.entity.UserAccountEntity;

import java.util.List;

/**
 * The simpler version of the Account class
 */
public final class UserAccountMapper {
    private UserAccountMapper() {
    }

    public static User toDto(UserAccountEntity entity) {
        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .avatarUrl(entity.getAvatarUrl())
                .applicationRoles(entity.getApplicationRoles() == null
                        ? List.of()
                        : List.copyOf(entity.getApplicationRoles()))
                .build();
    }
}
