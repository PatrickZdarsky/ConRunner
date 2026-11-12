package at.conrunner.shift.service.impl;

import at.conrunner.shift.auth.UserAttributeProvider;
import at.conrunner.shift.dao.AssignmentSwitchRequestDao;
import at.conrunner.shift.dao.ShiftPlanDao;
import at.conrunner.shift.dao.role.RoleDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.user.UserSearchDto;
import at.conrunner.shift.repo.AssignmentRepository;
import at.conrunner.shift.service.AssignmentService;
import at.conrunner.identity.User;
import at.conrunner.identity.UserService;
import at.conrunner.shift.util.SecurityHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {
    @Mock
    private VolunteerDao volunteerDao;

    @Mock
    private ShiftPlanDao shiftPlanDao;

    @Mock
    private UserService userDirectoryService;

    @Mock
    private UserAttributeProvider userAttributeProvider;

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private RoleDao roleDao;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private AssignmentSwitchRequestDao assignmentSwitchRequestDao;

    @Mock
    private AssignmentRepository assignmentRepository;

    @InjectMocks
    private UserAdministrationServiceImpl userAdministrationService;

    @Test
    void getAllUsers_filtersAndPaginatesUserInfos() {
        var alice = user(java.util.UUID.randomUUID(), "alice");
        var bob = user(java.util.UUID.randomUUID(), "bobby");
        when(userDirectoryService.searchUsers(org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(alice, bob), org.springframework.data.domain.PageRequest.of(0, 2), 3));
        when(volunteerDao.findAllByVolunteerIds(List.of(alice.getId(), bob.getId()))).thenReturn(List.of());

        var result = userAdministrationService.getAllUsers(0, 2, UserSearchDto.builder().name("o").build());

        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems())
            .extracting(dto -> dto.getVolunteer().getId(), dto -> dto.getVolunteer().getUsername())
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(alice.getId(), "alice"),
                org.assertj.core.groups.Tuple.tuple(bob.getId(), "bobby")
            );
    }

    @Test
    void getUser_mapsKnownAccountWhenLegacyVolunteerStateIsMissing() {
        var userId = java.util.UUID.randomUUID();
        var user = user(userId, "user.one");
        when(userDirectoryService.getUser(userId)).thenReturn(user);
        when(volunteerDao.findById(userId)).thenReturn(Optional.empty());

        var result = userAdministrationService.getUser(userId);

        assertThat(result.getVolunteer().getId()).isEqualTo(userId);
        assertThat(result.getPlanningPlans()).isEmpty();
        assertThat(result.getVolunteeringPlans()).isEmpty();
    }

    private static User user(java.util.UUID id, String username) {
        return User.builder().id(id).username(username).build();
    }
}
