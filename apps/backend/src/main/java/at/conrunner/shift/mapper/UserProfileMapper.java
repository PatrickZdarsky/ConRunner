package at.conrunner.shift.mapper;

import java.util.Collection;
import java.util.Collections;

import at.conrunner.shift.dto.role.RoleDto;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.dto.userprofile.UserProfileDto;
import at.conrunner.identity.User;

@RequiredArgsConstructor
@Service
public class UserProfileMapper {
    public static UserProfileDto toUserProfileDto(
        User user,
        Volunteer volunteer,
        String calendarShareId
    ) {
        Collection<RoleDto> roles = volunteer == null
            ? Collections.emptySet()
            : RoleMapper.toRoleDto(volunteer.getRoles());
        Collection<String> volunteeringPlans = volunteer == null
            ? Collections.emptySet()
            : ConvertUtil.toStringList(volunteer.getVolunteeringPlans().stream().map(ShiftPlan::getId));
        Collection<String> planningPlans = volunteer == null
            ? Collections.emptySet()
            : ConvertUtil.toStringList(volunteer.getPlanningPlans().stream().map(ShiftPlan::getId));
        Collection<String> volunteeringEvents = volunteer == null
            ? Collections.emptySet()
            : ConvertUtil.toStringList(volunteer
                .getVolunteeringPlans().stream()
                .map(ShiftPlan::getEvent)
                .map(Event::getId)
                .distinct());
        Collection<String> planningEvents = volunteer == null
            ? Collections.emptySet()
            : ConvertUtil.toStringList(volunteer
                .getPlanningPlans().stream()
                .map(ShiftPlan::getEvent)
                .map(Event::getId)
                .distinct());

        return UserProfileDto.builder()
            .account(AccountInfoMapper.toDto(user))
            .assignedRoles(roles)
            .volunteeringPlans(volunteeringPlans)
            .planningPlans(planningPlans)
            .volunteeringEvents(volunteeringEvents)
            .planningEvents(planningEvents)
            .calendarShareId(calendarShareId)
            .build();
    }
}
