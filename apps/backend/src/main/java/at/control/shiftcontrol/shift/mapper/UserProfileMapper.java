package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;
import java.util.Collections;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dto.userprofile.UserProfileDto;
import at.control.shiftcontrol.shift.userdirectory.UserInfo;

@RequiredArgsConstructor
@Service
public class UserProfileMapper {
    public static UserProfileDto toUserProfileDto(
        UserInfo user,
        Volunteer volunteer,
        String calendarShareId
    ) {
        Collection<at.control.shiftcontrol.shift.dto.role.RoleDto> roles = volunteer == null
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
