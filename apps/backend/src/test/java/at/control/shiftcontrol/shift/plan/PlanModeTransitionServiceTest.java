package at.control.shiftcontrol.shift.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.shift.assignment.AssignmentEligibility;
import at.control.shiftcontrol.shift.assignment.AssignmentEligibilityService;
import at.control.shiftcontrol.shift.assignment.AssignmentLifecycle;
import at.control.shiftcontrol.shift.assignment.AssignmentState;
import at.control.shiftcontrol.shift.assignment.StaffingAssignmentEntity;
import at.control.shiftcontrol.shift.assignment.StaffingAssignmentRepository;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftEntity;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileEntity;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanModeTransitionServiceTest {
    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    @Mock private ShiftPlanRepository shiftPlanRepository;
    @Mock private PlanModeTransitionRepository transitionRepository;
    @Mock private StaffingAssignmentRepository assignmentRepository;
    @Mock private PositionSlotRepository positionSlotRepository;
    @Mock private StaffingShiftRepository staffingShiftRepository;
    @Mock private AssignmentEligibilityService eligibilityService;
    @Mock private AssignmentLifecycle assignmentLifecycle;
    @Mock private VolunteerProfileService volunteerProfileService;
    @Mock private VolunteerProfileRepository volunteerProfileRepository;
    @Mock private PlanAccess planAccess;
    @Mock private Clock clock;

    @InjectMocks private PlanModeTransitionService transitions;

    @Test
    void refusesTheEntireTransitionWhenEveryJoinRequestCannotFit() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ShiftPlanEntity plan = plan(planId);
        StaffingAssignmentEntity request = assignment(slotId, userId, AssignmentState.JOIN_REQUESTED);
        PositionSlotEntity slot = slot(slotId, 1);
        StaffingShiftEntity staffingShift = staffingShift(slot.getStaffingShiftId(), planId);
        configurePlan(plan, request, List.of());
        when(volunteerProfileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(new VolunteerProfileEntity(userId, NOW)));
        when(positionSlotRepository.findAllByIdInForUpdate(List.of(slotId))).thenReturn(List.of(slot));
        when(staffingShiftRepository.findByIdForUpdate(slot.getStaffingShiftId())).thenReturn(Optional.of(staffingShift));
        when(eligibilityService.forPlanTransition(slot, staffingShift, plan, userId, null))
            .thenReturn(new AssignmentEligibility(slotId, userId, StaffingMode.SUPERVISED, 1, 1, Set.of()));

        assertThrows(ShiftConflictException.class, () -> transitions.change(planId, StaffingMode.SELF_SIGNUP));

        verify(assignmentLifecycle, never()).transition(any(), any(), any(), any());
        verify(shiftPlanRepository, never()).save(plan);
    }

    @Test
    void resolvesAllPendingJoinsAtomicallyWhenEnteringSelfSignup() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ShiftPlanEntity plan = plan(planId);
        StaffingAssignmentEntity request = assignment(slotId, userId, AssignmentState.JOIN_REQUESTED);
        PositionSlotEntity slot = slot(slotId, 1);
        StaffingShiftEntity staffingShift = staffingShift(slot.getStaffingShiftId(), planId);
        configurePlan(plan, request, List.of());
        when(volunteerProfileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(new VolunteerProfileEntity(userId, NOW)));
        when(positionSlotRepository.findAllByIdInForUpdate(List.of(slotId))).thenReturn(List.of(slot));
        when(staffingShiftRepository.findByIdForUpdate(slot.getStaffingShiftId())).thenReturn(Optional.of(staffingShift));
        when(eligibilityService.forPlanTransition(slot, staffingShift, plan, userId, null))
            .thenReturn(new AssignmentEligibility(slotId, userId, StaffingMode.SUPERVISED, 0, 1, Set.of()));
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(UUID.randomUUID(), false));
        when(shiftPlanRepository.save(plan)).thenReturn(plan);
        when(clock.instant()).thenReturn(NOW);

        ShiftPlanDetails result = transitions.change(planId, StaffingMode.SELF_SIGNUP);

        assertEquals(StaffingMode.SELF_SIGNUP, result.defaultStaffingMode());
        verify(assignmentLifecycle).transition(eq(request), eq(AssignmentState.ACCEPTED), any(UUID.class), any());
        verify(transitionRepository).save(any(PlanModeTransitionEntity.class));
    }

    private void configurePlan(ShiftPlanEntity plan, StaffingAssignmentEntity joinRequest,
                               List<StaffingAssignmentEntity> leaveRequests) {
        when(shiftPlanRepository.findByIdForUpdate(plan.getId())).thenReturn(Optional.of(plan));
        when(assignmentRepository.findAllByPlanIdAndStateForUpdate(plan.getId(), AssignmentState.JOIN_REQUESTED))
            .thenReturn(List.of(joinRequest));
        when(assignmentRepository.findAllByPlanIdAndStateForUpdate(plan.getId(), AssignmentState.LEAVE_REQUESTED))
            .thenReturn(leaveRequests);
    }

    private static ShiftPlanEntity plan(UUID planId) {
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setEventId(UUID.randomUUID());
        plan.setName("Operations");
        plan.setDefaultStaffingMode(StaffingMode.SUPERVISED);
        plan.setCreatedAt(NOW);
        return plan;
    }

    private static StaffingAssignmentEntity assignment(UUID slotId, UUID userId, AssignmentState state) {
        StaffingAssignmentEntity assignment = new StaffingAssignmentEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setPositionSlotId(slotId);
        assignment.setUserId(userId);
        assignment.setState(state);
        assignment.setCreatedAt(NOW);
        assignment.setUpdatedAt(NOW);
        return assignment;
    }

    private static PositionSlotEntity slot(UUID slotId, int desiredVolunteerCount) {
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setId(slotId);
        slot.setStaffingShiftId(UUID.randomUUID());
        slot.setDesiredVolunteerCount(desiredVolunteerCount);
        return slot;
    }

    private static StaffingShiftEntity staffingShift(UUID staffingShiftId, UUID planId) {
        StaffingShiftEntity staffingShift = new StaffingShiftEntity();
        staffingShift.setId(staffingShiftId);
        staffingShift.setPlanId(planId);
        staffingShift.setStartTime(NOW);
        staffingShift.setEndTime(NOW.plusSeconds(3600));
        return staffingShift;
    }
}
