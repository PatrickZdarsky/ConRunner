package at.control.shiftcontrol.shift.mapper;

import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.dto.userprofile.AccountInfoDto;
import at.control.shiftcontrol.identity.User;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AccountInfoMapper {
    public static AccountInfoDto toDto(User user) {
        return AccountInfoDto.builder()
            .volunteer(VolunteerAssemblingMapper.toDtoFromUser(user))
            .username(user.getUsername())
            .email(user.getEmail())
            .profile(user.getAvatarUrl())
            .isPlatformAdmin(user.getApplicationRoles().contains(at.control.shiftcontrol.identity.ApplicationRole.PLATFORM_ADMIN))
            .build();
    }
}
