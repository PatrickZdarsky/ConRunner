package at.control.shiftcontrol.shift.plan;

import java.time.Clock;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
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
    private final EventCatalogQuery eventCatalogQuery;
    private final PlanAccess planAccess;
    private final PlanModeTransitionService planModeTransitionService;
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

    public ShiftPlanDetails changeStaffingMode(UUID planId, StaffingMode mode) {
        return planModeTransitionService.change(planId, mode);
    }

    ShiftPlanEntity requirePlan(UUID planId) {
        return shiftPlanRepository.findById(planId)
            .orElseThrow(() -> new ShiftNotFoundException("Shift plan not found: " + planId));
    }

    static ShiftPlanDetails toDetails(ShiftPlanEntity plan) {
        return new ShiftPlanDetails(plan.getId(), plan.getEventId(), plan.getName(), plan.getShortDescription(),
            plan.getLongDescription(), plan.getDefaultStaffingMode(), plan.getCreatedAt(), plan.getArchivedAt());
    }
}
