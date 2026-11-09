package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;
import java.util.Collections;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.entity.Event;
import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.dto.userprofile.NotificationSettingsDto;
import at.shiftcontrol.shiftservice.dto.userprofile.UserProfileDto;
import at.shiftcontrol.shiftservice.userdirectory.UserInfo;

@RequiredArgsConstructor
@Service
public class UserProfileMapper {
    public static UserProfileDto toUserProfileDto(
        UserInfo user,
        Collection<NotificationSettingsDto> notificationSettings,
        Volunteer volunteer,
        String calendarShareId
    ) {
        Collection<at.shiftcontrol.shiftservice.dto.role.RoleDto> roles = volunteer == null
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
            .notifications(notificationSettings)
            .assignedRoles(roles)
            .volunteeringPlans(volunteeringPlans)
            .planningPlans(planningPlans)
            .volunteeringEvents(volunteeringEvents)
            .planningEvents(planningEvents)
            .calendarShareId(calendarShareId)
            .build();
    }
}
