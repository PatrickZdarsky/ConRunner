package at.conrunner.shift.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import at.conrunner.identity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import at.conrunner.shift.entity.Volunteer;
import at.conrunner.platform.exception.NotFoundException;
import at.conrunner.shift.dto.user.ContactInfoDto;
import at.conrunner.shift.dto.user.UserEventDto;
import at.conrunner.shift.dto.user.UserPlanDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAssemblingMapper {
    public static UserPlanDto toUserPlanDto(Volunteer volunteer, User user, Long planId) {
        return UserPlanDto.builder()
            .volunteer(VolunteerAssemblingMapper.toDtoFromUser(user))
            .email(user.getEmail())
            .roles(volunteer == null ? null : RoleMapper.toRoleDto(volunteer.getRoles(), planId))
            .isLocked(volunteer == null ? null : volunteer.getLockedPlans().stream().anyMatch(x -> planId.equals(x.getId())))
            .build();
    }

    public static Collection<UserPlanDto> toUserPlanDto(Collection<Volunteer> volunteers, Collection<User> users, Long planId) {
        Map<java.util.UUID, User> usersById = users.stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        return volunteers.stream()
            .map(v -> toUserPlanDto(v, getRequiredUser(usersById, v.getId()), planId))
            .sorted(UserPlanDto.nameComparator())
            .toList();
    }

    public static UserEventDto toUserEventDto(Volunteer volunteer, User user) {
        if (volunteer == null) {
            return UserEventDto.builder()
                .volunteer(VolunteerAssemblingMapper.toDtoFromUser(user))
                .volunteeringPlans(Collections.emptySet())
                .planningPlans(Collections.emptySet())
                .lockedPlans(Collections.emptySet())
                .email(user.getEmail())
                .build();
        }
        return UserEventDto.builder()
            .volunteer(VolunteerAssemblingMapper.toDtoFromUser(user))
            .email(user.getEmail())
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

    public static Collection<UserEventDto> toUserEventDtoForUsers(Collection<Volunteer> volunteers, Collection<User> users) {
        Map<java.util.UUID, Volunteer> volunteersById = volunteers.stream()
            .collect(Collectors.toMap(Volunteer::getId, Function.identity()));

        return users.stream()
            .map(u -> toUserEventDto(volunteersById.get(u.getId()), u))
            .sorted(UserEventDto.nameComparator())
            .toList();
    }

    public static ContactInfoDto toContactInfoDto(User user) {
        return ContactInfoDto.builder()
            .userId(user.getId())
            .name(user.getUsername())
            .email(user.getEmail())
            .build();
    }

    private static User getRequiredUser(Map<java.util.UUID, User> usersById, java.util.UUID userId) {
        User user = usersById.get(userId);
        if (user == null) {
            throw new NotFoundException();
        }

        return user;
    }
}
