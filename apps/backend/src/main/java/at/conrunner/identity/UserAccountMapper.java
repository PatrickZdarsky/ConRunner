package at.conrunner.identity;

import at.conrunner.identity.entity.UserAccountEntity;
import at.conrunner.identity.entity.UserAccountStatus;

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
                .emailVerified(entity.isEmailVerified())
                .active(entity.getStatus() == UserAccountStatus.ACTIVE)
                .avatarUrl(entity.getAvatarUrl())
                .applicationRoles(entity.getApplicationRoles() == null
                        ? List.of()
                        : List.copyOf(entity.getApplicationRoles()))
                .build();
    }
}
