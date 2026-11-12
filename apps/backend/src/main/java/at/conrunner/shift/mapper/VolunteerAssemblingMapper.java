package at.conrunner.shift.mapper;

import java.util.Collection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import at.conrunner.shift.dto.userprofile.VolunteerDto;
import at.conrunner.identity.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VolunteerAssemblingMapper {
    public static VolunteerDto toDtoFromUser(User user) {
        return VolunteerDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .name(user.getUsername())
            .profile(user.getAvatarUrl())
            .build();
    }

    public static Collection<VolunteerDto> toDtoFromUser(Collection<User> users) {
        return users.stream().map(VolunteerAssemblingMapper::toDtoFromUser).toList();
    }
}
