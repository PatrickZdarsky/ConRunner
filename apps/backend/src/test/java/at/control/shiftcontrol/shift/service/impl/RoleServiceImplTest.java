package at.control.shiftcontrol.shift.service.impl;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.Role;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.dao.ShiftPlanDao;
import at.control.shiftcontrol.shift.dao.role.RoleDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.role.UserRoleAssignmentAssignDto;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserService;
import at.control.shiftcontrol.shift.util.SecurityHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {
    @Mock
    private RoleDao roleDao;

    @Mock
    private ShiftPlanDao shiftPlanDao;

    @Mock
    private VolunteerDao volunteerDao;

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private UserService userDirectoryService;

    @Mock
    private at.control.shiftcontrol.communications.api.NotificationRequests notificationRequests;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void createUserRoleAssignment_returnsVolunteerDtoFromUserInfo() {
        Event event = new Event();
        event.setId(3L);
        ShiftPlan shiftPlan = new ShiftPlan();
        shiftPlan.setId(11L);
        shiftPlan.setEvent(event);
        Role role = new Role();
        role.setId(7L);
        role.setShiftPlan(shiftPlan);
        role.setName("Check-In");
        role.setSelfAssignable(true);
        Volunteer volunteer = new Volunteer();
        var userId = java.util.UUID.randomUUID();
        volunteer.setId(userId);
        volunteer.setRoles(new HashSet<>());

        when(roleDao.getById(7L)).thenReturn(role);
        when(volunteerDao.getById(userId)).thenReturn(volunteer);
        when(userDirectoryService.getUser(userId)).thenReturn(User.builder()
            .id(userId).username("alice").build());

        var result = roleService.createUserRoleAssignment(
            userId,
            UserRoleAssignmentAssignDto.builder().roleId("7").build()
        );

        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("alice");
        verify(volunteerDao).save(volunteer);
    }
}
