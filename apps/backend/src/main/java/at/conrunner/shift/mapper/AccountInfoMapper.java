package at.conrunner.shift.mapper;

import at.conrunner.identity.ApplicationRole;
import lombok.NoArgsConstructor;

import at.conrunner.shift.dto.userprofile.AccountInfoDto;
import at.conrunner.identity.User;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AccountInfoMapper {
    public static AccountInfoDto toDto(User user) {
        return AccountInfoDto.builder()
            .volunteer(VolunteerAssemblingMapper.toDtoFromUser(user))
            .username(user.getUsername())
            .email(user.getEmail())
            .profile(user.getAvatarUrl())
            .isPlatformAdmin(user.getApplicationRoles().contains(ApplicationRole.PLATFORM_ADMIN))
            .build();
    }
}
