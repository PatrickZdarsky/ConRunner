package at.control.shiftcontrol.shift.plan;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import at.control.shiftcontrol.audit.AuditLog;
import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.ShiftEntity;
import at.control.shiftcontrol.shift.ShiftRepository;
import at.control.shiftcontrol.shift.assignment.AssignmentEligibility;
import at.control.shiftcontrol.shift.assignment.AssignmentEligibilityService;
import at.control.shiftcontrol.shift.assignment.AssignmentEntity;
import at.control.shiftcontrol.shift.assignment.AssignmentNotEligibleException;
import at.control.shiftcontrol.shift.assignment.AssignmentRepository;
import at.control.shiftcontrol.shift.assignment.AssignmentStatus;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShiftPlanService {
    private final ShiftPlanRepository shiftPlanRepository;
    private final PlanMembershipRepository membershipRepository;
    private final VolunteerProfileService volunteerProfileService;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final EventCatalogQuery eventCatalogQuery;
    private final PlanAccess planAccess;
    private final AssignmentRepository assignmentRepository;
    private final PositionSlotRepository positionSlotRepository;
    private final ShiftRepository shiftRepository;
    private final AssignmentEligibilityService eligibilityService;
    private final AuditLog auditLog;
    private final Clock clock;

    @Transactional
    public ShiftPlanDetails create(UUID eventId, ShiftPlanData data) {
        planAccess.requirePlatformAdmin();
        eventCatalogQuery.requireEvent(eventId);
        if (shiftPlanRepository.existsByEventIdAndNameIgnoreCase(eventId, data.name())) {
            throw new ShiftConflictException("A shift plan with this name already exists in the event");
        }
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setEventId(eventId);
        plan.setName(data.name());
        plan.setShortDescription(data.shortDescription());
        plan.setLongDescription(data.longDescription());
        plan.setDefaultStaffingMode(StaffingMode.SELF_SIGNUP);
        plan.setCreatedAt(clock.instant());
        plan = shiftPlanRepository.save(plan);
        CurrentShiftActor.ShiftActor actor = planAccess.currentActor();
        volunteerProfileService.ensureProfile(actor.userId());
        membershipRepository.save(new PlanMembershipEntity(plan.getId(), actor.userId(), true, clock.instant()));
        auditLog.record(actor.userId(), "SHIFT_PLAN_CREATED", "SHIFT_PLAN", plan.getId(), null,
            StaffingMode.SELF_SIGNUP.name(), null);
        return toDetails(plan);
    }

    public ShiftPlanDetails get(UUID planId) {
        planAccess.requireMember(planId);
        return toDetails(requirePlan(planId));
    }

    public Collection<ShiftPlanDetails> listForEvent(UUID eventId) {
        eventCatalogQuery.requireEvent(eventId);
        CurrentShiftActor.ShiftActor actor = planAccess.currentActor();
        Collection<ShiftPlanEntity> plans = shiftPlanRepository.findAllByEventIdOrderByNameAsc(eventId);
        if (actor.platformAdmin()) {
            return plans.stream().map(ShiftPlanService::toDetails).toList();
        }
        HashSet<UUID> allowedPlanIds = membershipRepository.findAllByUserId(actor.userId()).stream()
            .map(PlanMembershipEntity::getPlanId)
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        return plans.stream().filter(plan -> allowedPlanIds.contains(plan.getId())).map(ShiftPlanService::toDetails).toList();
    }

    @Transactional
    public ShiftPlanDetails updateMetadata(UUID planId, ShiftPlanData data) {
        planAccess.requirePlanner(planId);
        ShiftPlanEntity plan = requirePlan(planId);
        if (!plan.getName().equalsIgnoreCase(data.name())
            && shiftPlanRepository.existsByEventIdAndNameIgnoreCase(plan.getEventId(), data.name())) {
            throw new ShiftConflictException("A shift plan with this name already exists in the event");
        }
        plan.setName(data.name());
        plan.setShortDescription(data.shortDescription());
        plan.setLongDescription(data.longDescription());
        return toDetails(shiftPlanRepository.save(plan));
    }

    /**
     * Retains the agreed atomic policy: entering SELF_SIGNUP accepts valid pending joins and
     * completes pending leaves, or fails as a whole. The audit log is the permanent history.
     */
    @Transactional
    public ShiftPlanDetails changeStaffingMode(UUID planId, StaffingMode targetMode) {
        if (targetMode == null) {
            throw new ShiftValidationException("Staffing mode is required");
        }
        planAccess.requirePlanner(planId);
        ShiftPlanEntity plan = shiftPlanRepository.findByIdForUpdate(planId)
            .orElseThrow(() -> new ShiftNotFoundException("Shift plan not found: " + planId));
        StaffingMode oldMode = plan.getDefaultStaffingMode();
        if (oldMode == targetMode) {
            return toDetails(plan);
        }

        List<AssignmentEntity> joinRequests = List.of();
        List<AssignmentEntity> leaveRequests = List.of();
        if (targetMode == StaffingMode.SELF_SIGNUP) {
            joinRequests = assignmentRepository.findAllByPlanIdAndStatus(planId, AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
            leaveRequests = assignmentRepository.findAllByPlanIdAndStatus(planId, AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN);
            Map<UUID, PositionSlotEntity> slots = lockedSlots(joinRequests);
            lockedShifts(slots.values());
            Map<UUID, AssignmentEntity> lockedAssignments = lockAssignments(joinRequests, leaveRequests);
            joinRequests = resolveLockedRequests(joinRequests, lockedAssignments, AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
            leaveRequests = resolveLockedRequests(leaveRequests, lockedAssignments, AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN);
            lockVolunteers(joinRequests);
            validateAutomaticJoinAcceptances(plan, joinRequests, slots);
        }

        UUID actor = planAccess.currentActor().userId();
        plan.setDefaultStaffingMode(targetMode);
        for (AssignmentEntity assignment : joinRequests) {
            assignment.setStatus(AssignmentStatus.ACCEPTED);
            assignment.setUpdatedAt(clock.instant());
            assignmentRepository.save(assignment);
            auditLog.record(actor, "ASSIGNMENT_JOIN_REQUEST_AUTO_ACCEPTED", "ASSIGNMENT", assignment.getId(),
                AssignmentStatus.REQUEST_FOR_ASSIGNMENT.name(), AssignmentStatus.ACCEPTED.name(),
                "Plan mode changed to SELF_SIGNUP");
        }
        for (AssignmentEntity assignment : leaveRequests) {
            assignmentRepository.delete(assignment);
            auditLog.record(actor, "ASSIGNMENT_LEAVE_REQUEST_AUTO_ACCEPTED", "ASSIGNMENT", assignment.getId(),
                AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN.name(), null, "Plan mode changed to SELF_SIGNUP");
        }
        ShiftPlanEntity saved = shiftPlanRepository.save(plan);
        String resolution = targetMode == StaffingMode.SELF_SIGNUP
            ? "ACCEPT_ALL_IF_VALID_AND_ACCEPT_ALL_LEAVES; joins=" + joinRequests.size() + "; leaves=" + leaveRequests.size()
            : "NO_REQUEST_RESOLUTION";
        auditLog.record(actor, "SHIFT_PLAN_STAFFING_MODE_CHANGED", "SHIFT_PLAN", saved.getId(), oldMode.name(),
            targetMode.name(), resolution);
        return toDetails(saved);
    }

    ShiftPlanEntity requirePlan(UUID planId) {
        return shiftPlanRepository.findById(planId)
            .orElseThrow(() -> new ShiftNotFoundException("Shift plan not found: " + planId));
    }

    private void validateAutomaticJoinAcceptances(ShiftPlanEntity plan, List<AssignmentEntity> joinRequests,
                                                  Map<UUID, PositionSlotEntity> slots) {
        if (joinRequests.isEmpty()) {
            return;
        }
        Map<UUID, ShiftEntity> shifts = lockedShifts(slots.values());
        Map<UUID, Integer> acceptedInTransition = new HashMap<>();
        Map<UUID, List<TimeRange>> requestedWindowsByVolunteer = new HashMap<>();
        for (AssignmentEntity request : joinRequests) {
            PositionSlotEntity slot = slots.get(request.getPositionSlotId());
            ShiftEntity shift = shifts.get(slot.getShiftId());
            AssignmentEligibility eligibility = eligibilityService.forPlanTransition(slot, shift, plan, request.getUserId());
            if (!eligibility.eligible()) {
                throw new AssignmentNotEligibleException(eligibility);
            }
            int accepted = acceptedInTransition.getOrDefault(slot.getId(), 0);
            if (eligibility.filledCount() + accepted >= slot.getDesiredVolunteerCount()) {
                throw new ShiftConflictException("Cannot enter SELF_SIGNUP: pending join requests exceed slot capacity");
            }
            TimeRange candidate = new TimeRange(shift.getStartTime().toEpochMilli(), shift.getEndTime().toEpochMilli());
            List<TimeRange> windows = requestedWindowsByVolunteer.computeIfAbsent(request.getUserId(), ignored -> new ArrayList<>());
            if (windows.stream().anyMatch(existing -> existing.overlaps(candidate))) {
                throw new ShiftConflictException("Cannot enter SELF_SIGNUP: one volunteer has overlapping join requests");
            }
            windows.add(candidate);
            acceptedInTransition.merge(slot.getId(), 1, Integer::sum);
        }
    }

    private Map<UUID, PositionSlotEntity> lockedSlots(Collection<AssignmentEntity> requests) {
        List<UUID> slotIds = requests.stream().map(AssignmentEntity::getPositionSlotId).distinct().sorted().toList();
        Map<UUID, PositionSlotEntity> slots = new HashMap<>();
        for (PositionSlotEntity slot : positionSlotRepository.findAllByIdInForUpdate(slotIds)) {
            slots.put(slot.getId(), slot);
        }
        if (slots.size() != slotIds.size()) {
            throw new ShiftNotFoundException("A position slot referenced by a join request no longer exists");
        }
        return slots;
    }

    private Map<UUID, ShiftEntity> lockedShifts(Collection<PositionSlotEntity> slots) {
        Map<UUID, ShiftEntity> shifts = new HashMap<>();
        for (UUID shiftId : slots.stream().map(PositionSlotEntity::getShiftId).distinct().sorted().toList()) {
            ShiftEntity shift = shiftRepository.findByIdForUpdate(shiftId)
                .orElseThrow(() -> new ShiftNotFoundException("Shift not found: " + shiftId));
            shifts.put(shiftId, shift);
        }
        return shifts;
    }

    private Map<UUID, AssignmentEntity> lockAssignments(Collection<AssignmentEntity> joinRequests,
                                                         Collection<AssignmentEntity> leaveRequests) {
        List<UUID> ids = java.util.stream.Stream.concat(joinRequests.stream(), leaveRequests.stream())
            .map(AssignmentEntity::getId).distinct().sorted().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, AssignmentEntity> assignments = new HashMap<>();
        for (AssignmentEntity assignment : assignmentRepository.findAllByIdInForUpdate(ids)) {
            assignments.put(assignment.getId(), assignment);
        }
        if (assignments.size() != ids.size()) {
            throw new ShiftConflictException("A pending assignment changed while the plan mode was being updated");
        }
        return assignments;
    }

    private static List<AssignmentEntity> resolveLockedRequests(Collection<AssignmentEntity> requested,
                                                                  Map<UUID, AssignmentEntity> locked,
                                                                  AssignmentStatus expectedStatus) {
        List<AssignmentEntity> resolved = requested.stream().map(AssignmentEntity::getId).map(locked::get).toList();
        if (resolved.stream().anyMatch(assignment -> assignment.getStatus() != expectedStatus)) {
            throw new ShiftConflictException("A pending assignment changed while the plan mode was being updated");
        }
        return resolved;
    }

    private void lockVolunteers(Collection<AssignmentEntity> requests) {
        for (UUID userId : requests.stream().map(AssignmentEntity::getUserId).distinct().sorted(Comparator.naturalOrder()).toList()) {
            volunteerProfileService.ensureProfile(userId);
            volunteerProfileRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ShiftNotFoundException("Volunteer profile not found: " + userId));
        }
    }

    static ShiftPlanDetails toDetails(ShiftPlanEntity plan) {
        return new ShiftPlanDetails(plan.getId(), plan.getEventId(), plan.getName(), plan.getShortDescription(),
            plan.getLongDescription(), plan.getDefaultStaffingMode(), plan.getCreatedAt(), plan.getArchivedAt());
    }

    private record TimeRange(long startEpochMillis, long endEpochMillis) {
        boolean overlaps(TimeRange other) {
            return startEpochMillis < other.endEpochMillis && endEpochMillis > other.startEpochMillis;
        }
    }
}
