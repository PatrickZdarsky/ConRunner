package at.control.shiftcontrol.shift.service.positionslot.impl;

import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.dao.AssignmentDao;
import at.control.shiftcontrol.shift.dao.PositionSlotDao;
import at.control.shiftcontrol.shift.dao.ShiftPlanDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;
import at.control.shiftcontrol.shift.mapper.AssignmentAssemblingMapper;
import at.control.shiftcontrol.shift.mapper.AssignmentPlannerInfoAssemblingMapper;
import at.control.shiftcontrol.shift.service.AssignmentService;
import at.control.shiftcontrol.shift.service.EligibilityService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerPositionSlotServiceImplTest {
    @Mock at.control.shiftcontrol.shift.audit.PlanningAudit planningAudit;
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
