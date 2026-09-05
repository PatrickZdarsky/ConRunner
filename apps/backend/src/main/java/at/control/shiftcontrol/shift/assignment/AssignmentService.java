package at.control.shiftcontrol.shift.assignment;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import at.control.shiftcontrol.audit.AuditLog;
import at.control.shiftcontrol.shift.ShiftEntity;
import at.control.shiftcontrol.shift.ShiftRepository;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.ShiftPlanRepository;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Live assignment workflow. Deletion represents a volunteer no longer occupying or requesting a slot. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final PositionSlotRepository positionSlotRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftPlanRepository shiftPlanRepository;
    private final VolunteerProfileService volunteerProfileService;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final PlanAccess planAccess;
    private final PlanMembershipRepository membershipRepository;
    private final AssignmentEligibilityService eligibilityService;
    private final AuditLog auditLog;
    private final Clock clock;

    /** Advisory only; mutating commands evaluate the same rules again while holding locks. */
    public AssignmentEligibility assessDirectJoin(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, false);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        return eligibilityService.forDirectJoin(context.slot(), context.shift(), context.plan(), volunteerId,
            assignmentRepository.findByPositionSlotIdAndUserId(positionSlotId, volunteerId).orElse(null));
    }

    @Transactional
    public AssignmentDecision directJoin(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        AssignmentEntity existing = lockAssignmentThenVolunteer(positionSlotId, volunteerId);
        AssignmentEligibility eligibility = eligibilityService.forDirectJoin(
            context.slot(), context.shift(), context.plan(), volunteerId, existing);
        requireEligible(eligibility, null, false);
        AssignmentEntity assignment = saveNew(positionSlotId, volunteerId, AssignmentStatus.ACCEPTED);
        audit(assignment, volunteerId, "ASSIGNMENT_JOINED", null, AssignmentStatus.ACCEPTED, null);
        return new AssignmentDecision(toDetails(assignment), eligibility, false);
    }

    @Transactional
    public AssignmentDetails directLeave(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        AssignmentEntity assignment = requireAssignmentThenLockVolunteer(positionSlotId, volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SELF_SIGNUP, "Direct leave is only available in SELF_SIGNUP mode");
        requireStatus(assignment, AssignmentStatus.ACCEPTED, "Volunteer has no accepted assignment to leave");
        AssignmentDetails details = toDetails(assignment);
        assignmentRepository.delete(assignment);
        audit(assignment, volunteerId, "ASSIGNMENT_LEFT", AssignmentStatus.ACCEPTED, null, null);
        return details;
    }

    @Transactional
    public AssignmentDetails requestJoin(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        AssignmentEntity existing = lockAssignmentThenVolunteer(positionSlotId, volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SUPERVISED, "Join requests are only available in SUPERVISED mode");
        AssignmentEligibility eligibility = eligibilityService.forSupervisedJoinRequest(
            context.slot(), context.shift(), context.plan(), volunteerId, existing);
        requireEligible(eligibility, null, false);
        AssignmentEntity assignment = saveNew(positionSlotId, volunteerId, AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
        audit(assignment, volunteerId, "ASSIGNMENT_JOIN_REQUESTED", null, AssignmentStatus.REQUEST_FOR_ASSIGNMENT, null);
        return toDetails(assignment);
    }

    @Transactional
    public AssignmentDetails withdrawJoinRequest(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        AssignmentEntity assignment = requireAssignmentThenLockVolunteer(positionSlotId, volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SUPERVISED, "Join requests are only available in SUPERVISED mode");
        requireStatus(assignment, AssignmentStatus.REQUEST_FOR_ASSIGNMENT, "Volunteer has no join request to withdraw");
        AssignmentDetails details = toDetails(assignment);
        assignmentRepository.delete(assignment);
        audit(assignment, volunteerId, "ASSIGNMENT_JOIN_REQUEST_WITHDRAWN", AssignmentStatus.REQUEST_FOR_ASSIGNMENT, null, null);
        return details;
    }

    @Transactional
    public AssignmentDetails requestLeave(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        AssignmentEntity assignment = requireAssignmentThenLockVolunteer(positionSlotId, volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SUPERVISED, "Leave requests are only available in SUPERVISED mode");
        requireStatus(assignment, AssignmentStatus.ACCEPTED, "Volunteer has no accepted assignment to request leaving");
        return changeStatus(assignment, volunteerId, "ASSIGNMENT_LEAVE_REQUESTED", AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN, null);
    }

    @Transactional
    public AssignmentDetails withdrawLeaveRequest(UUID positionSlotId) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requireMember(context.plan().getId());
        UUID volunteerId = planAccess.currentActor().userId();
        AssignmentEntity assignment = requireAssignmentThenLockVolunteer(positionSlotId, volunteerId);
        requireVolunteerChangesAllowed(context.plan().getId(), volunteerId);
        requireMode(context, StaffingMode.SUPERVISED, "Leave requests are only available in SUPERVISED mode");
        requireStatus(assignment, AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN, "Volunteer has no leave request to withdraw");
        return changeStatus(assignment, volunteerId, "ASSIGNMENT_LEAVE_REQUEST_WITHDRAWN", AssignmentStatus.ACCEPTED, null);
    }

    @Transactional
    public AssignmentDecision assign(UUID positionSlotId, UUID volunteerId, String overrideReason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        AssignmentEntity existing = lockAssignmentThenVolunteer(positionSlotId, volunteerId);
        if (existing != null && existing.getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT) {
            throw new ShiftConflictException("Volunteer already has an assignment in this position slot");
        }
        AssignmentEligibility eligibility = eligibilityService.forPlannerAssignment(
            context.slot(), context.shift(), context.plan(), volunteerId, existing);
        boolean overrideApplied = requireEligible(eligibility, overrideReason, true);
        UUID actor = planAccess.currentActor().userId();
        AssignmentEntity assignment;
        if (existing == null) {
            assignment = saveNew(positionSlotId, volunteerId, AssignmentStatus.ACCEPTED);
            audit(assignment, actor, "ASSIGNMENT_CREATED_BY_PLANNER", null, AssignmentStatus.ACCEPTED, overrideApplied ? overrideReason : null);
        } else {
            changeStatus(existing, actor, "ASSIGNMENT_JOIN_REQUEST_ACCEPTED", AssignmentStatus.ACCEPTED,
                overrideApplied ? overrideReason : null);
            assignment = existing;
        }
        return new AssignmentDecision(toDetails(assignment), eligibility, overrideApplied);
    }

    @Transactional
    public AssignmentDetails unassign(UUID positionSlotId, UUID volunteerId, String reason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        AssignmentEntity assignment = requireAssignmentThenLockVolunteer(positionSlotId, volunteerId);
        if (!AssignmentEligibilityService.OCCUPYING_STATUSES.contains(assignment.getStatus())) {
            throw new ShiftConflictException("Volunteer has no occupied assignment to unassign");
        }
        AssignmentDetails details = toDetails(assignment);
        assignmentRepository.delete(assignment);
        audit(assignment, planAccess.currentActor().userId(), "ASSIGNMENT_UNASSIGNED_BY_PLANNER", assignment.getStatus(), null, reason);
        return details;
    }

    @Transactional
    public AssignmentDecision acceptJoinRequest(UUID positionSlotId, UUID volunteerId, String overrideReason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        AssignmentEntity assignment = requireAssignmentThenLockVolunteer(positionSlotId, volunteerId);
        requireStatus(assignment, AssignmentStatus.REQUEST_FOR_ASSIGNMENT, "Volunteer has no join request to accept");
        AssignmentEligibility eligibility = eligibilityService.forPlannerAssignment(
            context.slot(), context.shift(), context.plan(), volunteerId, assignment);
        boolean overrideApplied = requireEligible(eligibility, overrideReason, true);
        changeStatus(assignment, planAccess.currentActor().userId(), "ASSIGNMENT_JOIN_REQUEST_ACCEPTED",
            AssignmentStatus.ACCEPTED, overrideApplied ? overrideReason : null);
        return new AssignmentDecision(toDetails(assignment), eligibility, overrideApplied);
    }

    @Transactional
    public AssignmentDetails declineJoinRequest(UUID positionSlotId, UUID volunteerId, String reason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        AssignmentEntity assignment = requireAssignmentThenLockVolunteer(positionSlotId, volunteerId);
        requireStatus(assignment, AssignmentStatus.REQUEST_FOR_ASSIGNMENT, "Volunteer has no join request to decline");
        AssignmentDetails details = toDetails(assignment);
        assignmentRepository.delete(assignment);
        audit(assignment, planAccess.currentActor().userId(), "ASSIGNMENT_JOIN_REQUEST_DECLINED",
            AssignmentStatus.REQUEST_FOR_ASSIGNMENT, null, reason);
        return details;
    }

    @Transactional
    public AssignmentDetails acceptLeaveRequest(UUID positionSlotId, UUID volunteerId, String reason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        AssignmentEntity assignment = requireAssignmentThenLockVolunteer(positionSlotId, volunteerId);
        requireStatus(assignment, AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN, "Volunteer has no leave request to accept");
        AssignmentDetails details = toDetails(assignment);
        assignmentRepository.delete(assignment);
        audit(assignment, planAccess.currentActor().userId(), "ASSIGNMENT_LEAVE_REQUEST_ACCEPTED",
            AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN, null, reason);
        return details;
    }

    @Transactional
    public AssignmentDetails declineLeaveRequest(UUID positionSlotId, UUID volunteerId, String reason) {
        AssignmentContext context = loadContext(positionSlotId, true);
        planAccess.requirePlanner(context.plan().getId());
        AssignmentEntity assignment = requireAssignmentThenLockVolunteer(positionSlotId, volunteerId);
        requireStatus(assignment, AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN, "Volunteer has no leave request to decline");
        return changeStatus(assignment, planAccess.currentActor().userId(), "ASSIGNMENT_LEAVE_REQUEST_DECLINED",
            AssignmentStatus.ACCEPTED, reason);
    }

    /** Called in the time-constraint transaction after its volunteer lock has been acquired. */
    @Transactional
    public int resolveEmergency(UUID eventId, UUID volunteerId, Instant startTime, Instant endTime, UUID actorUserId) {
        List<AssignmentEntity> assignments = assignmentRepository.findOccupiedOverlappingForUpdate(eventId, volunteerId,
            startTime, endTime, AssignmentEligibilityService.OCCUPYING_STATUSES);
        for (AssignmentEntity assignment : assignments) {
            PositionSlotEntity slot = positionSlotRepository.findById(assignment.getPositionSlotId())
                .orElseThrow(() -> new ShiftNotFoundException("Position slot not found: " + assignment.getPositionSlotId()));
            ShiftEntity shift = shiftRepository.findById(slot.getShiftId())
                .orElseThrow(() -> new ShiftNotFoundException("Shift not found: " + slot.getShiftId()));
            ShiftPlanEntity plan = shiftPlanRepository.findById(shift.getPlanId())
                .orElseThrow(() -> new ShiftNotFoundException("Shift plan not found: " + shift.getPlanId()));
            if (AssignmentEligibilityService.effectiveMode(shift, plan) == StaffingMode.SELF_SIGNUP) {
                AssignmentStatus previous = assignment.getStatus();
                assignmentRepository.delete(assignment);
                audit(assignment, actorUserId, "ASSIGNMENT_EMERGENCY_UNASSIGNED", previous, null,
                    "Conflicts with emergency time constraint");
            } else if (assignment.getStatus() != AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN) {
                changeStatus(assignment, actorUserId, "ASSIGNMENT_EMERGENCY_LEAVE_REQUESTED",
                    AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN, "Conflicts with emergency time constraint");
            }
        }
        return assignments.size();
    }

    private AssignmentContext loadContext(UUID positionSlotId, boolean forUpdate) {
        PositionSlotEntity initialSlot = positionSlotRepository.findById(positionSlotId)
            .orElseThrow(() -> new ShiftNotFoundException("Position slot not found: " + positionSlotId));
        ShiftEntity initialShift = shiftRepository.findById(initialSlot.getShiftId())
            .orElseThrow(() -> new ShiftNotFoundException("Shift not found: " + initialSlot.getShiftId()));
        ShiftPlanEntity plan = (forUpdate ? shiftPlanRepository.findByIdForUpdate(initialShift.getPlanId())
            : shiftPlanRepository.findById(initialShift.getPlanId()))
            .orElseThrow(() -> new ShiftNotFoundException("Shift plan not found: " + initialShift.getPlanId()));
        PositionSlotEntity slot = forUpdate ? positionSlotRepository.findByIdForUpdate(positionSlotId)
            .orElseThrow(() -> new ShiftNotFoundException("Position slot not found: " + positionSlotId)) : initialSlot;
        ShiftEntity shift = forUpdate ? shiftRepository.findByIdForUpdate(slot.getShiftId())
            .orElseThrow(() -> new ShiftNotFoundException("Shift not found: " + slot.getShiftId())) : initialShift;
        return new AssignmentContext(plan, shift, slot);
    }

    private AssignmentEntity lockAssignmentThenVolunteer(UUID positionSlotId, UUID volunteerId) {
        AssignmentEntity assignment = assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(positionSlotId, volunteerId)
            .orElse(null);
        lockVolunteer(volunteerId);
        return assignment;
    }

    private AssignmentEntity requireAssignmentThenLockVolunteer(UUID positionSlotId, UUID volunteerId) {
        AssignmentEntity assignment = assignmentRepository.findByPositionSlotIdAndUserIdForUpdate(positionSlotId, volunteerId)
            .orElseThrow(() -> new ShiftNotFoundException("Assignment not found for volunteer and position slot"));
        lockVolunteer(volunteerId);
        return assignment;
    }

    private void lockVolunteer(UUID volunteerId) {
        volunteerProfileService.ensureProfile(volunteerId);
        volunteerProfileRepository.findByIdForUpdate(volunteerId)
            .orElseThrow(() -> new ShiftNotFoundException("Volunteer profile not found: " + volunteerId));
    }

    private AssignmentEntity saveNew(UUID positionSlotId, UUID volunteerId, AssignmentStatus status) {
        AssignmentEntity assignment = new AssignmentEntity();
        assignment.setPositionSlotId(positionSlotId);
        assignment.setUserId(volunteerId);
        assignment.setStatus(status);
        assignment.setCreatedAt(clock.instant());
        assignment.setUpdatedAt(clock.instant());
        return assignmentRepository.save(assignment);
    }

    private AssignmentDetails changeStatus(AssignmentEntity assignment, UUID actor, String action, AssignmentStatus next, String reason) {
        AssignmentStatus previous = assignment.getStatus();
        assignment.setStatus(next);
        assignment.setUpdatedAt(clock.instant());
        assignment = assignmentRepository.save(assignment);
        audit(assignment, actor, action, previous, next, reason);
        return toDetails(assignment);
    }

    private void requireVolunteerChangesAllowed(UUID planId, UUID volunteerId) {
        if (membershipRepository.findByPlanIdAndUserId(planId, volunteerId)
            .map(at.control.shiftcontrol.shift.plan.PlanMembershipEntity::isVolunteerChangesRestricted).orElse(true)) {
            throw new ShiftConflictException("Volunteer changes are restricted for this plan membership");
        }
    }

    private static void requireMode(AssignmentContext context, StaffingMode expected, String message) {
        if (AssignmentEligibilityService.effectiveMode(context.shift(), context.plan()) != expected) {
            throw new ShiftConflictException(message);
        }
    }

    private static void requireStatus(AssignmentEntity assignment, AssignmentStatus expected, String message) {
        if (assignment.getStatus() != expected) {
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
            throw new ShiftValidationException("Reason must be at most 1024 characters");
        }
        return normalized;
    }

    private void audit(AssignmentEntity assignment, UUID actor, String action, AssignmentStatus previous,
                       AssignmentStatus next, String reason) {
        auditLog.record(actor, action, "ASSIGNMENT", assignment.getId(),
            previous == null ? null : previous.name(), next == null ? null : next.name(), normalizedReason(reason));
    }

    private static AssignmentDetails toDetails(AssignmentEntity assignment) {
        return new AssignmentDetails(assignment.getId(), assignment.getPositionSlotId(), assignment.getUserId(),
            assignment.getStatus(), assignment.getCreatedAt(), assignment.getUpdatedAt());
    }

    private record AssignmentContext(ShiftPlanEntity plan, ShiftEntity shift, PositionSlotEntity slot) {
    }
}
