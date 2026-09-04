package at.control.shiftcontrol.shift.assignment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.shift.availability.AvailabilityRepository;
import at.control.shiftcontrol.shift.plan.PlanMembershipEntity;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentEligibilityServiceTest {
    private static final Instant START = Instant.parse("2026-05-01T12:00:00Z");

    @Mock
    private PlanMembershipRepository membershipRepository;

    @Mock
    private StaffingAssignmentRepository assignmentRepository;

    @Mock
    private AvailabilityRepository availabilityRepository;

    @InjectMocks
    private AssignmentEligibilityService eligibilityService;

    @Test
    void directJoinReturnsStableReasonsForCapacityAndUnavailability() {
        UUID planId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        PositionSlotEntity slot = slot(1);
        StaffingShiftEntity staffingShift = staffingShift(planId);
        ShiftPlanEntity plan = plan(planId);
        when(membershipRepository.findByPlanIdAndUserId(planId, volunteerId))
            .thenReturn(Optional.of(new PlanMembershipEntity(planId, volunteerId, false, START)));
        when(assignmentRepository.countByPositionSlotIdAndStateIn(slot.getId(), AssignmentEligibilityService.OCCUPYING_STATES))
            .thenReturn(1L);
        when(assignmentRepository.existsOccupiedAssignmentOverlappingShift(volunteerId, START, START.plusSeconds(3600),
            AssignmentEligibilityService.OCCUPYING_STATES)).thenReturn(false);
        when(availabilityRepository.existsOverlapping(plan.getEventId(), volunteerId, START, START.plusSeconds(3600))).thenReturn(true);

        AssignmentEligibility eligibility = eligibilityService.forDirectJoin(slot, staffingShift, plan, volunteerId, null);

        assertFalse(eligibility.eligible());
        assertTrue(eligibility.reasons().contains(AssignmentEligibilityReason.SLOT_FULL));
        assertTrue(eligibility.reasons().contains(AssignmentEligibilityReason.UNAVAILABLE));
    }

    @Test
    void supervisedJoinRequestDoesNotReserveOrRequireCapacity() {
        UUID planId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        PositionSlotEntity slot = slot(1);
        StaffingShiftEntity staffingShift = staffingShift(planId);
        ShiftPlanEntity plan = plan(planId);
        when(membershipRepository.findByPlanIdAndUserId(planId, volunteerId))
            .thenReturn(Optional.of(new PlanMembershipEntity(planId, volunteerId, false, START)));
        when(assignmentRepository.countByPositionSlotIdAndStateIn(slot.getId(), AssignmentEligibilityService.OCCUPYING_STATES))
            .thenReturn(1L);
        when(assignmentRepository.existsOccupiedAssignmentOverlappingShift(volunteerId, START, START.plusSeconds(3600),
            AssignmentEligibilityService.OCCUPYING_STATES)).thenReturn(false);
        when(availabilityRepository.existsOverlapping(plan.getEventId(), volunteerId, START, START.plusSeconds(3600))).thenReturn(false);

        AssignmentEligibility eligibility = eligibilityService.forSupervisedJoinRequest(slot, staffingShift, plan, volunteerId, null);

        assertTrue(eligibility.eligible());
        assertFalse(eligibility.reasons().contains(AssignmentEligibilityReason.SLOT_FULL));
        assertTrue(eligibility.filledCount() == 1);
    }

    private static ShiftPlanEntity plan(UUID planId) {
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setEventId(UUID.randomUUID());
        plan.setDefaultStaffingMode(StaffingMode.SELF_SIGNUP);
        return plan;
    }

    private static StaffingShiftEntity staffingShift(UUID planId) {
        StaffingShiftEntity staffingShift = new StaffingShiftEntity();
        staffingShift.setId(UUID.randomUUID());
        staffingShift.setPlanId(planId);
        staffingShift.setStartTime(START);
        staffingShift.setEndTime(START.plusSeconds(3600));
        return staffingShift;
    }

    private static PositionSlotEntity slot(int desiredVolunteerCount) {
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setId(UUID.randomUUID());
        slot.setStaffingShiftId(UUID.randomUUID());
        slot.setDesiredVolunteerCount(desiredVolunteerCount);
        return slot;
    }
}
