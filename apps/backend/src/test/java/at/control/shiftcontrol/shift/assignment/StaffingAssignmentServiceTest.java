package at.control.shiftcontrol.shift.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.shift.plan.CurrentShiftActor;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.PlanMembershipEntity;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.ShiftPlanRepository;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftEntity;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileEntity;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffingAssignmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    @Mock private StaffingAssignmentRepository assignmentRepository;
    @Mock private PositionSlotRepository positionSlotRepository;
    @Mock private StaffingShiftRepository staffingShiftRepository;
    @Mock private ShiftPlanRepository shiftPlanRepository;
    @Mock private VolunteerProfileService volunteerProfileService;
    @Mock private VolunteerProfileRepository volunteerProfileRepository;
    @Mock private PlanAccess planAccess;
    @Mock private PlanMembershipRepository membershipRepository;
    @Mock private AssignmentEligibilityService eligibilityService;
    @Mock private AssignmentLifecycle assignmentLifecycle;
    @Mock private Clock clock;

    @InjectMocks private StaffingAssignmentService assignments;

    private UUID planId;
    private UUID slotId;
    private UUID volunteerId;
    private PositionSlotEntity slot;
    private StaffingShiftEntity staffingShift;
    private ShiftPlanEntity plan;

    @BeforeEach
    void setUpContext() {
        planId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        volunteerId = UUID.randomUUID();
        slot = new PositionSlotEntity();
        slot.setId(slotId);
        slot.setStaffingShiftId(UUID.randomUUID());
        slot.setDesiredVolunteerCount(1);
        staffingShift = new StaffingShiftEntity();
        staffingShift.setId(slot.getStaffingShiftId());
        staffingShift.setPlanId(planId);
        staffingShift.setStartTime(NOW);
        staffingShift.setEndTime(NOW.plusSeconds(3600));
        plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setEventId(UUID.randomUUID());
        plan.setDefaultStaffingMode(StaffingMode.SELF_SIGNUP);
        when(positionSlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(positionSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(staffingShiftRepository.findById(slot.getStaffingShiftId())).thenReturn(Optional.of(staffingShift));
        when(staffingShiftRepository.findByIdForUpdate(slot.getStaffingShiftId())).thenReturn(Optional.of(staffingShift));
        when(shiftPlanRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(volunteerId, false));
        when(volunteerProfileRepository.findByIdForUpdate(volunteerId))
            .thenReturn(Optional.of(new VolunteerProfileEntity(volunteerId, NOW)));
    }

    @Test
    void directJoinReturnsTheLockedEligibilityDecisionAndCreatesAnAcceptedAssignment() {
        AssignmentEligibility eligible = new AssignmentEligibility(slotId, volunteerId, StaffingMode.SELF_SIGNUP, 0, 1, Set.of());
        prepareTransition();
        when(assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(slotId, volunteerId)).thenReturn(Optional.empty());
        when(eligibilityService.forDirectJoin(slot, staffingShift, plan, volunteerId, null)).thenReturn(eligible);

        AssignmentDecision decision = assignments.directJoin(slotId);

        assertTrue(decision.eligibility().eligible());
        assertEquals(AssignmentState.ACCEPTED, decision.assignment().state());
        verify(planAccess).requireMember(planId);
    }

    @Test
    void directJoinReturnsMachineReadableFailureInsteadOfParsingAConflictMessage() {
        AssignmentEligibility ineligible = new AssignmentEligibility(slotId, volunteerId, StaffingMode.SELF_SIGNUP, 1, 1,
            Set.of(AssignmentEligibilityReason.SLOT_FULL));
        when(assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(slotId, volunteerId)).thenReturn(Optional.empty());
        when(eligibilityService.forDirectJoin(slot, staffingShift, plan, volunteerId, null)).thenReturn(ineligible);

        AssignmentNotEligibleException exception = assertThrows(AssignmentNotEligibleException.class,
            () -> assignments.directJoin(slotId));

        assertTrue(exception.getEligibility().reasons().contains(AssignmentEligibilityReason.SLOT_FULL));
    }

    @Test
    void plannerAssignmentAllowsOnlyAnExplicitRecordedOverride() {
        UUID plannerId = UUID.randomUUID();
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(plannerId, false));
        when(volunteerProfileRepository.findByIdForUpdate(volunteerId))
            .thenReturn(Optional.of(new VolunteerProfileEntity(volunteerId, NOW)));
        AssignmentEligibility unavailable = new AssignmentEligibility(slotId, volunteerId, StaffingMode.LOCKED, 0, 1,
            Set.of(AssignmentEligibilityReason.UNAVAILABLE));
        when(assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(slotId, volunteerId)).thenReturn(Optional.empty());
        when(eligibilityService.forPlannerAssignment(slot, staffingShift, plan, volunteerId, null)).thenReturn(unavailable);
        prepareTransition();

        AssignmentDecision decision = assignments.assign(slotId, volunteerId, "Emergency coverage approved");

        assertTrue(decision.overrideApplied());
        assertEquals("Emergency coverage approved", decision.assignment().authorizedOverrideReason());
        verify(planAccess).requirePlanner(planId);
        verify(assignmentLifecycle).transition(any(StaffingAssignmentEntity.class), eq(AssignmentState.ACCEPTED), eq(plannerId),
            eq("Emergency coverage approved"));
    }

    @Test
    void supervisedJoinRequestPersistsARequestWithoutOccupyingTheSlot() {
        plan.setDefaultStaffingMode(StaffingMode.SUPERVISED);
        when(membershipRepository.findByPlanIdAndUserId(planId, volunteerId))
            .thenReturn(Optional.of(new PlanMembershipEntity(planId, volunteerId, false, NOW)));
        when(assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(slotId, volunteerId)).thenReturn(Optional.empty());
        when(eligibilityService.forSupervisedJoinRequest(slot, staffingShift, plan, volunteerId, null))
            .thenReturn(new AssignmentEligibility(slotId, volunteerId, StaffingMode.SUPERVISED, 1, 1, Set.of()));
        prepareTransition();

        AssignmentDetails request = assignments.requestJoin(slotId);

        assertEquals(AssignmentState.JOIN_REQUESTED, request.state());
        verify(assignmentLifecycle).transition(any(StaffingAssignmentEntity.class), eq(AssignmentState.JOIN_REQUESTED),
            eq(volunteerId), eq(null));
    }

    @Test
    void supervisedLeaveRequestRetainsTheAcceptedAssignmentAsOccupied() {
        plan.setDefaultStaffingMode(StaffingMode.SUPERVISED);
        when(membershipRepository.findByPlanIdAndUserId(planId, volunteerId))
            .thenReturn(Optional.of(new PlanMembershipEntity(planId, volunteerId, false, NOW)));
        StaffingAssignmentEntity accepted = new StaffingAssignmentEntity();
        accepted.setId(UUID.randomUUID());
        accepted.setPositionSlotId(slotId);
        accepted.setUserId(volunteerId);
        accepted.setState(AssignmentState.ACCEPTED);
        accepted.setCreatedAt(NOW);
        accepted.setUpdatedAt(NOW);
        when(assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(slotId, volunteerId)).thenReturn(Optional.of(accepted));
        prepareExistingTransition();

        AssignmentDetails request = assignments.requestLeave(slotId);

        assertEquals(AssignmentState.LEAVE_REQUESTED, request.state());
        verify(assignmentLifecycle).transition(accepted, AssignmentState.LEAVE_REQUESTED, volunteerId, null);
    }

    private void prepareTransition() {
        when(clock.instant()).thenReturn(NOW);
        prepareExistingTransition();
    }

    private void prepareExistingTransition() {
        when(assignmentLifecycle.transition(any(StaffingAssignmentEntity.class), any(AssignmentState.class), any(UUID.class), any()))
            .thenAnswer(invocation -> {
                StaffingAssignmentEntity assignment = invocation.getArgument(0);
                assignment.setState(invocation.getArgument(1));
                return assignment;
            });
    }
}
