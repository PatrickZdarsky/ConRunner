package at.control.shiftcontrol.shift.plan;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.shift.assignment.AssignmentEligibility;
import at.control.shiftcontrol.shift.assignment.AssignmentLifecycle;
import at.control.shiftcontrol.shift.assignment.AssignmentNotEligibleException;
import at.control.shiftcontrol.shift.assignment.AssignmentState;
import at.control.shiftcontrol.shift.assignment.StaffingAssignmentEntity;
import at.control.shiftcontrol.shift.assignment.StaffingAssignmentRepository;
import at.control.shiftcontrol.shift.assignment.AssignmentEligibilityService;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftEntity;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates a plan-level policy transition. It validates every automatic acceptance before
 * changing any workflow state, so a failed transition cannot partially resolve requests.
 */
@Service
@RequiredArgsConstructor
public class PlanModeTransitionService {
    private static final String SELF_SIGNUP_POLICY = "ACCEPT_ALL_IF_VALID_AND_ACCEPT_ALL_LEAVES";
    private static final String NO_RESOLUTION_POLICY = "NO_REQUEST_RESOLUTION";

    private final ShiftPlanRepository shiftPlanRepository;
    private final PlanModeTransitionRepository transitionRepository;
    private final StaffingAssignmentRepository assignmentRepository;
    private final PositionSlotRepository positionSlotRepository;
    private final StaffingShiftRepository staffingShiftRepository;
    private final AssignmentEligibilityService eligibilityService;
    private final AssignmentLifecycle assignmentLifecycle;
    private final VolunteerProfileService volunteerProfileService;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final PlanAccess planAccess;
    private final Clock clock;

    @Transactional
    public ShiftPlanDetails change(UUID planId, StaffingMode targetMode) {
        if (targetMode == null) {
            throw new ShiftValidationException("Staffing mode is required");
        }
        planAccess.requirePlanner(planId);
        ShiftPlanEntity plan = shiftPlanRepository.findByIdForUpdate(planId)
            .orElseThrow(() -> new ShiftNotFoundException("Shift plan not found: " + planId));
        StaffingMode oldMode = plan.getDefaultStaffingMode();
        if (oldMode == targetMode) {
            return ShiftPlanService.toDetails(plan);
        }

        List<StaffingAssignmentEntity> joinRequests = List.of();
        List<StaffingAssignmentEntity> leaveRequests = List.of();
        String policy = NO_RESOLUTION_POLICY;
        if (targetMode == StaffingMode.SELF_SIGNUP) {
            joinRequests = assignmentRepository.findAllByPlanIdAndStateForUpdate(planId, AssignmentState.JOIN_REQUESTED);
            leaveRequests = assignmentRepository.findAllByPlanIdAndStateForUpdate(planId, AssignmentState.LEAVE_REQUESTED);
            lockVolunteers(joinRequests);
            validateAutomaticJoinAcceptances(plan, joinRequests);
            policy = SELF_SIGNUP_POLICY;
        }

        plan.setDefaultStaffingMode(targetMode);
        UUID actorUserId = planAccess.currentActor().userId();
        for (StaffingAssignmentEntity assignment : joinRequests) {
            assignment.setAuthorizedOverrideReason(null);
            assignmentLifecycle.transition(assignment, AssignmentState.ACCEPTED, actorUserId,
                "Automatically accepted by plan mode transition to SELF_SIGNUP");
        }
        for (StaffingAssignmentEntity assignment : leaveRequests) {
            assignmentLifecycle.transition(assignment, AssignmentState.UNASSIGNED, actorUserId,
                "Automatically accepted by plan mode transition to SELF_SIGNUP");
        }
        ShiftPlanEntity savedPlan = shiftPlanRepository.save(plan);
        recordTransition(savedPlan, oldMode, targetMode, policy, joinRequests.size(), leaveRequests.size(), actorUserId);
        return ShiftPlanService.toDetails(savedPlan);
    }

    private void validateAutomaticJoinAcceptances(ShiftPlanEntity plan, List<StaffingAssignmentEntity> joinRequests) {
        if (joinRequests.isEmpty()) {
            return;
        }
        Map<UUID, PositionSlotEntity> slots = lockedSlots(joinRequests);
        Map<UUID, StaffingShiftEntity> shifts = lockedShifts(slots.values());
        Map<UUID, Integer> acceptedInThisTransition = new HashMap<>();
        Map<UUID, List<TimeRange>> requestedWindowsByVolunteer = new HashMap<>();

        for (StaffingAssignmentEntity request : joinRequests) {
            PositionSlotEntity slot = slots.get(request.getPositionSlotId());
            StaffingShiftEntity staffingShift = shifts.get(slot.getStaffingShiftId());
            AssignmentEligibility eligibility = eligibilityService.forPlanTransition(
                slot, staffingShift, plan, request.getUserId(), null);
            if (!eligibility.eligible()) {
                throw new AssignmentNotEligibleException(eligibility);
            }

            int alreadyAccepted = acceptedInThisTransition.getOrDefault(slot.getId(), 0);
            if (eligibility.filledCount() + alreadyAccepted >= slot.getDesiredVolunteerCount()) {
                throw new ShiftConflictException("Cannot enter SELF_SIGNUP: pending join requests exceed slot capacity");
            }
            List<TimeRange> requestedWindows = requestedWindowsByVolunteer.computeIfAbsent(request.getUserId(), ignored -> new ArrayList<>());
            TimeRange candidate = new TimeRange(staffingShift.getStartTime().toEpochMilli(), staffingShift.getEndTime().toEpochMilli());
            if (requestedWindows.stream().anyMatch(existing -> existing.overlaps(candidate))) {
                throw new ShiftConflictException("Cannot enter SELF_SIGNUP: one volunteer has overlapping join requests");
            }
            requestedWindows.add(candidate);
            acceptedInThisTransition.merge(slot.getId(), 1, Integer::sum);
        }
    }

    private Map<UUID, PositionSlotEntity> lockedSlots(Collection<StaffingAssignmentEntity> requests) {
        List<UUID> slotIds = requests.stream()
            .map(StaffingAssignmentEntity::getPositionSlotId)
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();
        Map<UUID, PositionSlotEntity> slots = new HashMap<>();
        for (PositionSlotEntity slot : positionSlotRepository.findAllByIdInForUpdate(slotIds)) {
            slots.put(slot.getId(), slot);
        }
        if (slots.size() != slotIds.size()) {
            throw new ShiftNotFoundException("A position slot referenced by a join request no longer exists");
        }
        return slots;
    }

    private void lockVolunteers(Collection<StaffingAssignmentEntity> requests) {
        for (UUID userId : requests.stream().map(StaffingAssignmentEntity::getUserId).distinct().sorted().toList()) {
            volunteerProfileService.ensureProfile(userId);
            volunteerProfileRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ShiftNotFoundException("Volunteer profile not found: " + userId));
        }
    }

    private Map<UUID, StaffingShiftEntity> lockedShifts(Collection<PositionSlotEntity> slots) {
        Set<UUID> shiftIds = new HashSet<>();
        for (PositionSlotEntity slot : slots) {
            shiftIds.add(slot.getStaffingShiftId());
        }
        Map<UUID, StaffingShiftEntity> shifts = new HashMap<>();
        for (UUID shiftId : shiftIds.stream().sorted().toList()) {
            StaffingShiftEntity staffingShift = staffingShiftRepository.findByIdForUpdate(shiftId)
                .orElseThrow(() -> new ShiftNotFoundException("Staffing shift not found: " + shiftId));
            shifts.put(shiftId, staffingShift);
        }
        return shifts;
    }

    private void recordTransition(ShiftPlanEntity plan, StaffingMode oldMode, StaffingMode newMode, String policy,
                                  int joins, int leaves, UUID actorUserId) {
        PlanModeTransitionEntity transition = new PlanModeTransitionEntity();
        transition.setPlanId(plan.getId());
        transition.setActorUserId(actorUserId);
        transition.setOldMode(oldMode);
        transition.setNewMode(newMode);
        transition.setApplicationPolicy(policy);
        transition.setResolvedJoinRequestCount(joins);
        transition.setResolvedLeaveRequestCount(leaves);
        transition.setOccurredAt(clock.instant());
        transitionRepository.save(transition);
    }

    private record TimeRange(long startEpochMillis, long endEpochMillis) {
        boolean overlaps(TimeRange other) {
            return startEpochMillis < other.endEpochMillis && endEpochMillis > other.startEpochMillis;
        }
    }
}
