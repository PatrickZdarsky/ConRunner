package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;
import at.control.shiftcontrol.shift.userdirectory.UserInfo;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VolunteerAssemblingMapper {
    public static VolunteerDto toDtoFromUser(UserInfo user) {
        return VolunteerDto.builder()
            .id(user.id())
            .username(user.username())
            .firstName(user.firstName())
            .lastName(user.lastName())
            .profile(user.profile())
            .build();
    }

    public static Collection<VolunteerDto> toDtoFromUser(Collection<UserInfo> users) {
        return users.stream().map(VolunteerAssemblingMapper::toDtoFromUser).toList();
    }
}
