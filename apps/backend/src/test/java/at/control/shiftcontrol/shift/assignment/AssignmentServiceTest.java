package at.control.shiftcontrol.shift.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.audit.AuditLog;
import at.control.shiftcontrol.shift.ShiftEntity;
import at.control.shiftcontrol.shift.ShiftRepository;
import at.control.shiftcontrol.shift.plan.CurrentShiftActor;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.PlanMembershipEntity;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.ShiftPlanRepository;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileEntity;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private PositionSlotRepository positionSlotRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private ShiftPlanRepository shiftPlanRepository;
    @Mock private VolunteerProfileService volunteerProfileService;
    @Mock private VolunteerProfileRepository volunteerProfileRepository;
    @Mock private PlanAccess planAccess;
    @Mock private PlanMembershipRepository membershipRepository;
    @Mock private AssignmentEligibilityService eligibilityService;
    @Mock private AuditLog auditLog;
    @Mock private Clock clock;
    @InjectMocks private AssignmentService assignments;

    @Test
    void directLeaveDeletesTheLiveAssignmentAndAuditsIt() {
        Context context = context(StaffingMode.SELF_SIGNUP);
        AssignmentEntity assignment = assignment(context.slot().getId(), context.userId(), AssignmentStatus.ACCEPTED);
        configureContext(context, assignment, true);

        AssignmentDetails left = assignments.directLeave(context.slot().getId());

        assertEquals(AssignmentStatus.ACCEPTED, left.status());
        verify(assignmentRepository).delete(assignment);
        verify(auditLog).record(eq(context.userId()), eq("ASSIGNMENT_LEFT"), eq("ASSIGNMENT"), eq(assignment.getId()),
            eq("ACCEPTED"), eq(null), eq(null));
    }

    @Test
    void supervisedJoinCreatesTheLegacyRequestState() {
        Context context = context(StaffingMode.SUPERVISED);
        configureContext(context, null, true);
        when(eligibilityService.forSupervisedJoinRequest(any(), any(), any(), any(), eq(null)))
            .thenReturn(new AssignmentEligibility(context.slot().getId(), context.userId(), StaffingMode.SUPERVISED, 0, 1, java.util.Set.of()));
        when(clock.instant()).thenReturn(NOW);
        when(assignmentRepository.save(any(AssignmentEntity.class))).thenAnswer(invocation -> {
            AssignmentEntity assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        AssignmentDetails requested = assignments.requestJoin(context.slot().getId());

        assertEquals(AssignmentStatus.REQUEST_FOR_ASSIGNMENT, requested.status());
    }

    @Test
    void directJoinCreatesAcceptedLiveAssignmentFromTheLockedDecision() {
        Context context = context(StaffingMode.SELF_SIGNUP);
        configureContext(context, null, false);
        AssignmentEligibility eligibility = new AssignmentEligibility(context.slot().getId(), context.userId(),
            StaffingMode.SELF_SIGNUP, 0, 1, java.util.Set.of());
        when(eligibilityService.forDirectJoin(any(), any(), any(), any(), eq(null))).thenReturn(eligibility);
        when(clock.instant()).thenReturn(NOW);
        when(assignmentRepository.save(any(AssignmentEntity.class))).thenAnswer(invocation -> {
            AssignmentEntity assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        AssignmentDecision decision = assignments.directJoin(context.slot().getId());

        assertEquals(AssignmentStatus.ACCEPTED, decision.assignment().status());
        assertEquals(eligibility, decision.eligibility());
    }

    @Test
    void plannerMayOverrideOnlyAnExplicitOverrideableEligibilityFailure() {
        Context context = context(StaffingMode.LOCKED);
        configureContext(context, null, false);
        AssignmentEligibility blocked = new AssignmentEligibility(context.slot().getId(), context.userId(), StaffingMode.LOCKED,
            0, 1, java.util.Set.of(AssignmentEligibilityReason.UNAVAILABLE));
        when(eligibilityService.forPlannerAssignment(any(), any(), any(), any(), eq(null))).thenReturn(blocked);
        when(clock.instant()).thenReturn(NOW);
        when(assignmentRepository.save(any(AssignmentEntity.class))).thenAnswer(invocation -> {
            AssignmentEntity assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        AssignmentDecision assigned = assignments.assign(context.slot().getId(), context.userId(), "Operations approved");

        assertEquals(true, assigned.overrideApplied());
        assertEquals(AssignmentStatus.ACCEPTED, assigned.assignment().status());
        assertThrows(AssignmentNotEligibleException.class,
            () -> assignments.assign(context.slot().getId(), context.userId(), null));
    }

    private void configureContext(Context context, AssignmentEntity assignment, boolean volunteerChangeCheck) {
        when(positionSlotRepository.findById(context.slot().getId())).thenReturn(Optional.of(context.slot()));
        when(positionSlotRepository.findByIdForUpdate(context.slot().getId())).thenReturn(Optional.of(context.slot()));
        when(shiftRepository.findById(context.shift().getId())).thenReturn(Optional.of(context.shift()));
        when(shiftRepository.findByIdForUpdate(context.shift().getId())).thenReturn(Optional.of(context.shift()));
        when(shiftPlanRepository.findByIdForUpdate(context.plan().getId())).thenReturn(Optional.of(context.plan()));
        when(assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(context.slot().getId(), context.userId()))
            .thenReturn(Optional.ofNullable(assignment));
        when(volunteerProfileRepository.findByIdForUpdate(context.userId()))
            .thenReturn(Optional.of(new VolunteerProfileEntity(context.userId(), NOW)));
        if (volunteerChangeCheck) {
            when(membershipRepository.findByPlanIdAndUserId(context.plan().getId(), context.userId()))
                .thenReturn(Optional.of(new PlanMembershipEntity(context.plan().getId(), context.userId(), false, NOW)));
        }
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(context.userId(), false));
    }

    private static Context context(StaffingMode mode) {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setDefaultStaffingMode(mode);
        ShiftEntity shift = new ShiftEntity();
        shift.setId(UUID.randomUUID());
        shift.setPlanId(planId);
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setId(UUID.randomUUID());
        slot.setShiftId(shift.getId());
        return new Context(plan, shift, slot, userId);
    }

    private static AssignmentEntity assignment(UUID slotId, UUID userId, AssignmentStatus status) {
        AssignmentEntity assignment = new AssignmentEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setPositionSlotId(slotId);
        assignment.setUserId(userId);
        assignment.setStatus(status);
        assignment.setCreatedAt(NOW);
        assignment.setUpdatedAt(NOW);
        return assignment;
    }

    private record Context(ShiftPlanEntity plan, ShiftEntity shift, PositionSlotEntity slot, UUID userId) {
    }
}
