package at.control.shiftcontrol.shift.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.shift.ShiftEntity;
import at.control.shiftcontrol.shift.TimeConstraintRepository;
import at.control.shiftcontrol.shift.plan.PlanMembershipEntity;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentEligibilityServiceTest {
    @Mock private PlanMembershipRepository membershipRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private TimeConstraintRepository timeConstraintRepository;
    @InjectMocks private AssignmentEligibilityService eligibilityService;

    @Test
    void exposesLegacySummaryAndMachineReadableConstraintReason() {
        UUID planId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        PositionSlotEntity slot = slot();
        ShiftEntity shift = shift(planId);
        ShiftPlanEntity plan = plan(planId);
        PlanMembershipEntity membership = new PlanMembershipEntity(planId, volunteerId, false, Instant.now());
        when(membershipRepository.findByPlanIdAndUserId(planId, volunteerId)).thenReturn(Optional.of(membership));
        when(assignmentRepository.countByPositionSlotIdAndStatusIn(any(), any())).thenReturn(0L);
        when(assignmentRepository.existsOccupiedAssignmentOverlappingShift(any(), any(), any(), any())).thenReturn(false);
        when(timeConstraintRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(true);

        AssignmentEligibility result = eligibilityService.forDirectJoin(slot, shift, plan, volunteerId, null);

        assertEquals(PositionSignupState.TIME_CONFLICT_TIME_CONSTRAINT, result.signupState());
        assertEquals(true, result.reasons().contains(AssignmentEligibilityReason.UNAVAILABLE));
    }

    @Test
    void acceptedAssignmentIsReportedAsSignedUpRatherThanReusableHistory() {
        UUID planId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        AssignmentEntity existing = new AssignmentEntity();
        existing.setStatus(AssignmentStatus.ACCEPTED);
        when(membershipRepository.findByPlanIdAndUserId(planId, volunteerId))
            .thenReturn(Optional.of(new PlanMembershipEntity(planId, volunteerId, false, Instant.now())));
        when(assignmentRepository.countByPositionSlotIdAndStatusIn(any(), any())).thenReturn(1L);
        when(timeConstraintRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(false);

        AssignmentEligibility result = eligibilityService.forDirectJoin(slot(), shift(planId), plan(planId), volunteerId, existing);

        assertEquals(PositionSignupState.SIGNED_UP, result.signupState());
    }

    private static PositionSlotEntity slot() {
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setId(UUID.randomUUID());
        slot.setDesiredVolunteerCount(2);
        return slot;
    }

    private static ShiftEntity shift(UUID planId) {
        ShiftEntity shift = new ShiftEntity();
        shift.setPlanId(planId);
        shift.setStartTime(Instant.parse("2026-05-01T10:00:00Z"));
        shift.setEndTime(Instant.parse("2026-05-01T11:00:00Z"));
        return shift;
    }

    private static ShiftPlanEntity plan(UUID planId) {
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setEventId(UUID.randomUUID());
        plan.setDefaultStaffingMode(StaffingMode.SELF_SIGNUP);
        return plan;
    }
}
