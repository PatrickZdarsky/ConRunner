package at.conrunner.shift.service.positionslot.impl;

import at.conrunner.shift.audit.PlanningAudit;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.dao.AssignmentDao;
import at.conrunner.shift.dao.PositionSlotDao;
import at.conrunner.shift.dao.ShiftPlanDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.userprofile.VolunteerDto;
import at.conrunner.shift.mapper.AssignmentAssemblingMapper;
import at.conrunner.shift.mapper.AssignmentPlannerInfoAssemblingMapper;
import at.conrunner.shift.service.AssignmentService;
import at.conrunner.shift.service.EligibilityService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerPositionSlotServiceImplTest {
    @Mock
    PlanningAudit planningAudit;
    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private ShiftPlanDao shiftPlanDao;

    @Mock
    private AssignmentDao assignmentDao;

    @Mock
    private PositionSlotDao positionSlotDao;

    @Mock
    private VolunteerDao volunteerDao;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private EligibilityService eligibilityService;

    @Mock
    private UserService userDirectoryService;

    @Mock
    private AssignmentAssemblingMapper assignmentAssemblingMapper;

    @Mock
    private AssignmentPlannerInfoAssemblingMapper assignmentRequestAssemblingMapper;

    @InjectMocks
    private PlannerPositionSlotServiceImpl plannerPositionSlotService;

    @Test
    void getAssignableUsers_filtersVolunteersBeforeResolvingDirectoryProfiles() {
        ShiftPlan shiftPlan = new ShiftPlan();
        shiftPlan.setId(9L);
        Shift shift = new Shift();
        shift.setId(4L);
        shift.setShiftPlan(shiftPlan);
        PositionSlot positionSlot = new PositionSlot();
        positionSlot.setId(5L);
        positionSlot.setShift(shift);

        Volunteer eligibleVolunteer = new Volunteer();
        var userId = java.util.UUID.randomUUID();
        eligibleVolunteer.setId(userId);
        Volunteer ineligibleVolunteer = new Volunteer();
        ineligibleVolunteer.setId(java.util.UUID.randomUUID());

        when(positionSlotDao.getById(5L)).thenReturn(positionSlot);
        when(volunteerDao.findAllByShiftPlan(9L)).thenReturn(List.of(eligibleVolunteer, ineligibleVolunteer));
        when(eligibilityService.isEligibleAndNotSignedUp(positionSlot, eligibleVolunteer)).thenReturn(true);
        when(eligibilityService.isEligibleAndNotSignedUp(positionSlot, ineligibleVolunteer)).thenReturn(false);
        when(eligibilityService.getConflictingAssignmentsExcludingShift(userId, positionSlot, 4L)).thenReturn(List.of());
        when(userDirectoryService.getUsers(List.of(userId))).thenReturn(List.of(
            User.builder().id(userId).username("alice").build()
        ));

        var result = plannerPositionSlotService.getAssignableUsers("5");

        assertThat(result)
            .extracting(VolunteerDto::getId, VolunteerDto::getUsername)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(userId, "alice"));
        verify(securityHelper).assertUserIsPlanner(positionSlot);
    }
}
