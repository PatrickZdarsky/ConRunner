package at.control.shiftcontrol.shift.assignment;

import java.time.Clock;
import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.CurrentShiftActor;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.ShiftPlanRepository;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftEntity;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assignment workflow commands. Every mutation locks the plan before evaluating the effective mode. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StaffingAssignmentService {
    private final StaffingAssignmentRepository assignmentRepository;
    private final PositionSlotRepository positionSlotRepository;
    private final StaffingShiftRepository staffingShiftRepository;
    private final ShiftPlanRepository shiftPlanRepository;
    private final VolunteerProfileService volunteerProfileService;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final PlanAccess planAccess;
    private final PlanMembershipRepository membershipRepository;
    private final AssignmentEligibilityService eligibilityService;
    private final AssignmentLifecycle assignmentLifecycle;
    private final Clock clock;

    /** Advisory preflight. The same decision is recomputed under locks by {@link #directJoin(UUID)}. */
    public AssignmentEligibility assessDirectJoin(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, false);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        StaffingAssignmentEntity existing = assignmentRepository.findByPositionSlotIdAndUserId(positionSlotId, volunteerId)
            .orElse(null);
        return eligibilityService.forDirectJoin(context.slot(), context.staffingShift(), context.plan(), volunteerId, existing);
    }

    @Transactional
    public AssignmentDecision directJoin(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        lockVolunteer(volunteerId);
        StaffingAssignmentEntity existing = assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(positionSlotId, volunteerId)
            .orElse(null);
        AssignmentEligibility eligibility = eligibilityService.forDirectJoin(
            context.slot(), context.staffingShift(), context.plan(), volunteerId, existing);
        requireEligible(eligibility, null, false);
        StaffingAssignmentEntity assignment = existing == null ? newAssignment(positionSlotId, volunteerId) : existing;
        assignment.setAuthorizedOverrideReason(null);
        assignment = assignmentLifecycle.transition(assignment, AssignmentState.ACCEPTED, volunteerId, null);
        return new AssignmentDecision(toDetails(assignment), eligibility, false);
    }

    @Transactional
    public AssignmentDetails directLeave(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        lockVolunteer(volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SELF_SIGNUP, "Direct leave is only available in SELF_SIGNUP mode");
        StaffingAssignmentEntity assignment = requireAssignmentForUpdate(positionSlotId, volunteerId);
        requireState(assignment, AssignmentState.ACCEPTED, "Volunteer has no accepted assignment to leave");
        assignment = assignmentLifecycle.transition(assignment, AssignmentState.UNASSIGNED, volunteerId, null);
        return toDetails(assignment);
    }

    @Transactional
    public AssignmentDetails requestJoin(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        lockVolunteer(volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SUPERVISED, "Join requests are only available in SUPERVISED mode");
        StaffingAssignmentEntity existing = assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(positionSlotId, volunteerId)
            .orElse(null);
        AssignmentEligibility eligibility = eligibilityService.forSupervisedJoinRequest(
            context.slot(), context.staffingShift(), context.plan(), volunteerId, existing);
        requireEligible(eligibility, null, false);
        StaffingAssignmentEntity assignment = existing == null ? newAssignment(positionSlotId, volunteerId) : existing;
        assignment.setAuthorizedOverrideReason(null);
        return toDetails(assignmentLifecycle.transition(assignment, AssignmentState.JOIN_REQUESTED, volunteerId, null));
    }

    @Transactional
    public AssignmentDetails withdrawJoinRequest(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        lockVolunteer(volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SUPERVISED, "Join requests are only available in SUPERVISED mode");
        StaffingAssignmentEntity assignment = requireAssignmentForUpdate(positionSlotId, volunteerId);
        requireState(assignment, AssignmentState.JOIN_REQUESTED, "Volunteer has no join request to withdraw");
        return toDetails(assignmentLifecycle.transition(assignment, AssignmentState.WITHDRAWN, volunteerId, null));
    }

    @Transactional
    public AssignmentDetails requestLeave(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        lockVolunteer(volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SUPERVISED, "Leave requests are only available in SUPERVISED mode");
        StaffingAssignmentEntity assignment = requireAssignmentForUpdate(positionSlotId, volunteerId);
        requireState(assignment, AssignmentState.ACCEPTED, "Volunteer has no accepted assignment to request leaving");
        return toDetails(assignmentLifecycle.transition(assignment, AssignmentState.LEAVE_REQUESTED, volunteerId, null));
    }

    @Transactional
    public AssignmentDetails withdrawLeaveRequest(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        lockVolunteer(volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SUPERVISED, "Leave requests are only available in SUPERVISED mode");
        StaffingAssignmentEntity assignment = requireAssignmentForUpdate(positionSlotId, volunteerId);
        requireState(assignment, AssignmentState.LEAVE_REQUESTED, "Volunteer has no leave request to withdraw");
        return toDetails(assignmentLifecycle.transition(assignment, AssignmentState.ACCEPTED, volunteerId, null));
    }

    @Transactional
    public AssignmentDecision assign(UUID positionSlotId, UUID volunteerId, String overrideReason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        lockVolunteer(volunteerId);
        StaffingAssignmentEntity existing = assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(positionSlotId, volunteerId)
            .orElse(null);
        AssignmentEligibility eligibility = eligibilityService.forPlannerAssignment(
            context.slot(), context.staffingShift(), context.plan(), volunteerId, existing);
        boolean overrideApplied = requireEligible(eligibility, overrideReason, true);
        StaffingAssignmentEntity assignment = existing == null ? newAssignment(positionSlotId, volunteerId) : existing;
        assignment.setAuthorizedOverrideReason(overrideApplied ? normalizedReason(overrideReason) : null);
        assignment = assignmentLifecycle.transition(assignment, AssignmentState.ACCEPTED, planAccess.currentActor().userId(),
            assignment.getAuthorizedOverrideReason());
        return new AssignmentDecision(toDetails(assignment), eligibility, overrideApplied);
    }

    @Transactional
    public AssignmentDetails unassign(UUID positionSlotId, UUID volunteerId, String reason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        lockVolunteer(volunteerId);
        StaffingAssignmentEntity assignment = requireAssignmentForUpdate(positionSlotId, volunteerId);
        if (assignment.getState() != AssignmentState.ACCEPTED && assignment.getState() != AssignmentState.LEAVE_REQUESTED) {
            throw new ShiftConflictException("Volunteer has no occupied assignment to unassign");
        }
        return toDetails(assignmentLifecycle.transition(assignment, AssignmentState.UNASSIGNED,
            planAccess.currentActor().userId(), optionalReason(reason)));
    }

    @Transactional
    public AssignmentDecision acceptJoinRequest(UUID positionSlotId, UUID volunteerId, String overrideReason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        lockVolunteer(volunteerId);
        StaffingAssignmentEntity assignment = requireAssignmentForUpdate(positionSlotId, volunteerId);
        requireState(assignment, AssignmentState.JOIN_REQUESTED, "Volunteer has no join request to accept");
        AssignmentEligibility eligibility = eligibilityService.forPlannerAssignment(
            context.slot(), context.staffingShift(), context.plan(), volunteerId, null);
        boolean overrideApplied = requireEligible(eligibility, overrideReason, true);
        assignment.setAuthorizedOverrideReason(overrideApplied ? normalizedReason(overrideReason) : null);
        assignment = assignmentLifecycle.transition(assignment, AssignmentState.ACCEPTED, planAccess.currentActor().userId(),
            assignment.getAuthorizedOverrideReason());
        return new AssignmentDecision(toDetails(assignment), eligibility, overrideApplied);
    }

    @Transactional
    public AssignmentDetails declineJoinRequest(UUID positionSlotId, UUID volunteerId, String reason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        StaffingAssignmentEntity assignment = requireAssignmentForUpdate(positionSlotId, volunteerId);
        requireState(assignment, AssignmentState.JOIN_REQUESTED, "Volunteer has no join request to decline");
        return toDetails(assignmentLifecycle.transition(assignment, AssignmentState.DECLINED,
            planAccess.currentActor().userId(), optionalReason(reason)));
    }

    @Transactional
    public AssignmentDetails acceptLeaveRequest(UUID positionSlotId, UUID volunteerId, String reason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        StaffingAssignmentEntity assignment = requireAssignmentForUpdate(positionSlotId, volunteerId);
        requireState(assignment, AssignmentState.LEAVE_REQUESTED, "Volunteer has no leave request to accept");
        return toDetails(assignmentLifecycle.transition(assignment, AssignmentState.UNASSIGNED,
            planAccess.currentActor().userId(), optionalReason(reason)));
    }

    @Transactional
    public AssignmentDetails declineLeaveRequest(UUID positionSlotId, UUID volunteerId, String reason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        StaffingAssignmentEntity assignment = requireAssignmentForUpdate(positionSlotId, volunteerId);
        requireState(assignment, AssignmentState.LEAVE_REQUESTED, "Volunteer has no leave request to decline");
        return toDetails(assignmentLifecycle.transition(assignment, AssignmentState.ACCEPTED,
            planAccess.currentActor().userId(), optionalReason(reason)));
    }

    private AssignmentContext loadContext(UUID positionSlotId, boolean forUpdate) {
        PositionSlotEntity initialSlot = positionSlotRepository.findById(positionSlotId)
            .orElseThrow(() -> new ShiftNotFoundException("Position slot not found: " + positionSlotId));
        StaffingShiftEntity initialShift = staffingShiftRepository.findById(initialSlot.getStaffingShiftId())
            .orElseThrow(() -> new ShiftNotFoundException("Staffing shift not found: " + initialSlot.getStaffingShiftId()));
        ShiftPlanEntity plan = forUpdate
            ? shiftPlanRepository.findByIdForUpdate(initialShift.getPlanId())
                .orElseThrow(() -> new ShiftNotFoundException("Shift plan not found: " + initialShift.getPlanId()))
            : shiftPlanRepository.findById(initialShift.getPlanId())
                .orElseThrow(() -> new ShiftNotFoundException("Shift plan not found: " + initialShift.getPlanId()));
        PositionSlotEntity slot = forUpdate
            ? positionSlotRepository.findByIdForUpdate(positionSlotId)
                .orElseThrow(() -> new ShiftNotFoundException("Position slot not found: " + positionSlotId))
            : initialSlot;
        StaffingShiftEntity staffingShift = forUpdate
            ? staffingShiftRepository.findByIdForUpdate(slot.getStaffingShiftId())
                .orElseThrow(() -> new ShiftNotFoundException("Staffing shift not found: " + slot.getStaffingShiftId()))
            : initialShift;
        return new AssignmentContext(plan, staffingShift, slot);
    }

    private void lockVolunteer(UUID volunteerId) {
        volunteerProfileService.ensureProfile(volunteerId);
        volunteerProfileRepository.findByIdForUpdate(volunteerId)
            .orElseThrow(() -> new ShiftNotFoundException("Volunteer profile not found: " + volunteerId));
    }

    private StaffingAssignmentEntity newAssignment(UUID positionSlotId, UUID volunteerId) {
        StaffingAssignmentEntity assignment = new StaffingAssignmentEntity();
        assignment.setPositionSlotId(positionSlotId);
        assignment.setUserId(volunteerId);
        assignment.setCreatedAt(clock.instant());
        assignment.setUpdatedAt(clock.instant());
        return assignment;
    }

    private StaffingAssignmentEntity requireAssignmentForUpdate(UUID positionSlotId, UUID volunteerId) {
        return assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(positionSlotId, volunteerId)
            .orElseThrow(() -> new ShiftNotFoundException("Assignment not found for volunteer and position slot"));
    }

    private void requireVolunteerChangesAllowed(UUID planId, UUID volunteerId) {
        if (membershipRepository.findByPlanIdAndUserId(planId, volunteerId)
            .map(at.control.shiftcontrol.shift.plan.PlanMembershipEntity::isVolunteerChangesRestricted)
            .orElse(true)) {
            throw new ShiftConflictException("Volunteer changes are restricted for this plan membership");
        }
    }

    private static void requireMode(AssignmentContext context, StaffingMode expected, String message) {
        if (AssignmentEligibilityService.effectiveMode(context.staffingShift(), context.plan()) != expected) {
            throw new ShiftConflictException(message);
        }
    }

    private static void requireState(StaffingAssignmentEntity assignment, AssignmentState expected, String message) {
        if (assignment.getState() != expected) {
            throw new ShiftConflictException(message);
        }
    }

    private static boolean requireEligible(AssignmentEligibility eligibility, String overrideReason, boolean plannerMayOverride) {
        if (eligibility.eligible()) {
            return false;
        }
        if (plannerMayOverride && AssignmentEligibilityService.onlyOverrideable(eligibility.reasons())
            && normalizedReason(overrideReason) != null) {
            return true;
        }
        throw new AssignmentNotEligibleException(eligibility);
    }

    private static String normalizedReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String normalized = reason.trim();
        if (normalized.length() > 1024) {
            throw new ShiftValidationException("Override reason must be at most 1024 characters");
        }
        return normalized;
    }

    private static String optionalReason(String reason) {
        return normalizedReason(reason);
    }

    private static AssignmentDetails toDetails(StaffingAssignmentEntity assignment) {
        return new AssignmentDetails(assignment.getId(), assignment.getPositionSlotId(), assignment.getUserId(),
            assignment.getState(), assignment.getAuthorizedOverrideReason(), assignment.getCreatedAt(), assignment.getUpdatedAt());
    }

    private record AssignmentContext(ShiftPlanEntity plan, StaffingShiftEntity staffingShift, PositionSlotEntity slot) {
    }
}
