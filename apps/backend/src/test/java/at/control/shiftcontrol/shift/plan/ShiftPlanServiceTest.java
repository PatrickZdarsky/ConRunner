package at.control.shiftcontrol.shift.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.audit.AuditLog;
import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.ShiftRepository;
import at.control.shiftcontrol.shift.ShiftEntity;
import at.control.shiftcontrol.shift.assignment.AssignmentEligibility;
import at.control.shiftcontrol.shift.assignment.AssignmentEligibilityService;
import at.control.shiftcontrol.shift.assignment.AssignmentEntity;
import at.control.shiftcontrol.shift.assignment.AssignmentRepository;
import at.control.shiftcontrol.shift.assignment.AssignmentStatus;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftPlanServiceTest {
    @Mock private ShiftPlanRepository shiftPlanRepository;
    @Mock private PlanMembershipRepository membershipRepository;
    @Mock private VolunteerProfileService volunteerProfileService;
    @Mock private VolunteerProfileRepository volunteerProfileRepository;
    @Mock private EventCatalogQuery eventCatalogQuery;
    @Mock private PlanAccess planAccess;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private PositionSlotRepository positionSlotRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private AssignmentEligibilityService eligibilityService;
    @Mock private AuditLog auditLog;
    @Mock private Clock clock;
    @InjectMocks private ShiftPlanService plans;

    @Test
    void enteringSelfSignupRecordsTheAtomicResolutionPolicyEvenWithNoRequests() {
        UUID planId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setEventId(UUID.randomUUID());
        plan.setName("Plan");
        plan.setDefaultStaffingMode(StaffingMode.SUPERVISED);
        plan.setCreatedAt(Instant.parse("2026-05-01T10:00:00Z"));
        when(shiftPlanRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(assignmentRepository.findAllByPlanIdAndStatus(planId, at.control.shiftcontrol.shift.assignment.AssignmentStatus.REQUEST_FOR_ASSIGNMENT))
            .thenReturn(List.of());
        when(assignmentRepository.findAllByPlanIdAndStatus(planId, at.control.shiftcontrol.shift.assignment.AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN))
            .thenReturn(List.of());
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(actorId, false));
        when(shiftPlanRepository.save(any(ShiftPlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShiftPlanDetails result = plans.changeStaffingMode(planId, StaffingMode.SELF_SIGNUP);

        assertEquals(StaffingMode.SELF_SIGNUP, result.defaultStaffingMode());
        verify(auditLog).record(actorId, "SHIFT_PLAN_STAFFING_MODE_CHANGED", "SHIFT_PLAN", planId,
            "SUPERVISED", "SELF_SIGNUP", "ACCEPT_ALL_IF_VALID_AND_ACCEPT_ALL_LEAVES; joins=0; leaves=0");
    }

    @Test
    void enteringSelfSignupAcceptsPendingJoinsAndDeletesPendingLeavesInOneCommand() {
        UUID planId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ShiftPlanEntity plan = plan(planId, StaffingMode.SUPERVISED);
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setId(UUID.randomUUID());
        slot.setShiftId(UUID.randomUUID());
        slot.setDesiredVolunteerCount(2);
        ShiftEntity shift = new ShiftEntity();
        shift.setId(slot.getShiftId());
        shift.setPlanId(planId);
        shift.setStartTime(Instant.parse("2026-05-01T10:00:00Z"));
        shift.setEndTime(Instant.parse("2026-05-01T11:00:00Z"));
        AssignmentEntity join = assignment(slot.getId(), userId, AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
        AssignmentEntity leave = assignment(slot.getId(), UUID.randomUUID(), AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN);
        when(shiftPlanRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(assignmentRepository.findAllByPlanIdAndStatus(planId, AssignmentStatus.REQUEST_FOR_ASSIGNMENT)).thenReturn(List.of(join));
        when(assignmentRepository.findAllByPlanIdAndStatus(planId, AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN)).thenReturn(List.of(leave));
        when(assignmentRepository.findAllByIdInForUpdate(List.of(join.getId(), leave.getId()).stream().sorted().toList()))
            .thenReturn(List.of(join, leave));
        when(volunteerProfileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(new at.control.shiftcontrol.shift.volunteer.VolunteerProfileEntity(userId, Instant.now())));
        when(positionSlotRepository.findAllByIdInForUpdate(List.of(slot.getId()))).thenReturn(List.of(slot));
        when(shiftRepository.findByIdForUpdate(slot.getShiftId())).thenReturn(Optional.of(shift));
        when(eligibilityService.forPlanTransition(slot, shift, plan, userId))
            .thenReturn(new AssignmentEligibility(slot.getId(), userId, StaffingMode.SUPERVISED, 0, 2, java.util.Set.of()));
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(actorId, false));
        when(clock.instant()).thenReturn(Instant.parse("2026-05-01T09:00:00Z"));
        when(shiftPlanRepository.save(any(ShiftPlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        plans.changeStaffingMode(planId, StaffingMode.SELF_SIGNUP);

        assertEquals(AssignmentStatus.ACCEPTED, join.getStatus());
        verify(assignmentRepository).delete(leave);
    }

    private static ShiftPlanEntity plan(UUID planId, StaffingMode mode) {
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setEventId(UUID.randomUUID());
        plan.setName("Plan");
        plan.setDefaultStaffingMode(mode);
        plan.setCreatedAt(Instant.parse("2026-05-01T10:00:00Z"));
        return plan;
    }

    private static AssignmentEntity assignment(UUID slotId, UUID userId, AssignmentStatus status) {
        AssignmentEntity assignment = new AssignmentEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setPositionSlotId(slotId);
        assignment.setUserId(userId);
        assignment.setStatus(status);
        return assignment;
    }
}
