package at.control.shiftcontrol.shift.mapper;

import lombok.NoArgsConstructor;

import at.shiftcontrol.shiftservice.dto.userprofile.AccountInfoDto;
import at.shiftcontrol.shiftservice.userdirectory.UserInfo;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AccountInfoMapper {
    public static AccountInfoDto toDto(UserInfo user) {
        return AccountInfoDto.builder()
            .volunteer(VolunteerAssemblingMapper.toDtoFromUser(user))
            .username(user.username())
            .email(user.email())
            .profile(user.profile())
            .isPlatformAdmin(user.isPlatformAdmin())
            .build();
    }
}
