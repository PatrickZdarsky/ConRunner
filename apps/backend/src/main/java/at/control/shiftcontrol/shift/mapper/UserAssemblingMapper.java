package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import at.control.shiftcontrol.identity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.platform.exception.NotFoundException;
import at.control.shiftcontrol.shift.dto.user.ContactInfoDto;
import at.control.shiftcontrol.shift.dto.user.UserEventDto;
import at.control.shiftcontrol.shift.dto.user.UserPlanDto;
import at.control.shiftcontrol.shift.userdirectory.UserInfo;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAssemblingMapper {
    public static UserPlanDto toUserPlanDto(Volunteer volunteer, UserInfo user, Long planId) {
        return UserPlanDto.builder()
            .volunteer(VolunteerAssemblingMapper.toDtoFromUser(user))
            .email(user.email())
            .roles(volunteer == null ? null : RoleMapper.toRoleDto(volunteer.getRoles(), planId))
            .isLocked(volunteer == null ? null : volunteer.getLockedPlans().stream().anyMatch(x -> planId.equals(x.getId())))
            .build();
    }

    public static Collection<UserPlanDto> toUserPlanDto(Collection<Volunteer> volunteers, Collection<UserInfo> users, Long planId) {
        Map<String, UserInfo> usersById = users.stream()
            .collect(Collectors.toMap(UserInfo::id, Function.identity()));

        return volunteers.stream()
            .map(v -> toUserPlanDto(v, getRequiredUser(usersById, v.getId()), planId))
            .sorted(UserPlanDto.lastNameComparator())
            .toList();
    }

    public static UserEventDto toUserEventDto(Volunteer volunteer, UserInfo user) {
        if (volunteer == null) {
            return UserEventDto.builder()
                .volunteer(VolunteerAssemblingMapper.toDtoFromUser(user))
                .volunteeringPlans(Collections.emptySet())
                .planningPlans(Collections.emptySet())
                .lockedPlans(Collections.emptySet())
                .email(user.email())
                .build();
        }
        return UserEventDto.builder()
            .volunteer(VolunteerAssemblingMapper.toDtoFromUser(user))
            .email(user.email())
            .volunteeringPlans(volunteer.getVolunteeringPlans()
                .stream()
                .map(shiftPlan -> String.valueOf(shiftPlan.getId()))
                .toList())
            .planningPlans(volunteer.getPlanningPlans()
                .stream()
                .map(shiftPlan -> String.valueOf(shiftPlan.getId()))
                .toList())
            .lockedPlans(volunteer.getLockedPlans()
                .stream()
                .map(shiftPlan -> String.valueOf(shiftPlan.getId()))
                .toList())
            .build();
    }

    public static Collection<UserEventDto> toUserEventDtoForUsers(Collection<Volunteer> volunteers, Collection<UserInfo> users) {
        Map<String, Volunteer> volunteersById = volunteers.stream()
            .collect(Collectors.toMap(Volunteer::getId, Function.identity()));

        return users.stream()
            .map(u -> toUserEventDto(volunteersById.get(u.id()), u))
            .sorted(UserEventDto.lastNameComparator())
            .toList();
    }

    public static ContactInfoDto toContactInfoDto(User user) {
        return ContactInfoDto.builder()
            .userId(user.getId())
            .name(user.getUsername())
            .email(user.getEmail())
            .build();
    }

    private static UserInfo getRequiredUser(Map<String, UserInfo> usersById, String userId) {
        UserInfo user = usersById.get(userId);
        if (user == null) {
            throw new NotFoundException();
        }

        return user;
    }
}
