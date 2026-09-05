package at.control.shiftcontrol.shift;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.ShiftModeOverride;
import at.control.shiftcontrol.shift.plan.ShiftPlanDetails;
import at.control.shiftcontrol.shift.plan.ShiftPlanService;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShiftService {
    private final ShiftRepository shiftRepository;
    private final ShiftPlanService shiftPlanService;
    private final PlanAccess planAccess;
    private final EventCatalogQuery eventCatalogQuery;
    private final Clock clock;

    @Transactional
    public ShiftDetails create(UUID planId, ShiftData data) {
        planAccess.requirePlanner(planId);
        ShiftPlanDetails plan = shiftPlanService.get(planId);
        ShiftEntity shift = new ShiftEntity();
        shift.setPlanId(planId);
        shift.setCreatedAt(clock.instant());
        apply(data, shift, plan);
        return toDetails(shiftRepository.save(shift), plan.defaultStaffingMode());
    }

    public ShiftDetails get(UUID shiftId) {
        ShiftEntity shift = requireShift(shiftId);
        planAccess.requireMember(shift.getPlanId());
        return toDetails(shift, shiftPlanService.get(shift.getPlanId()).defaultStaffingMode());
    }

    public Collection<ShiftDetails> listForPlan(UUID planId) {
        planAccess.requirePlanner(planId);
        ShiftPlanDetails plan = shiftPlanService.get(planId);
        return shiftRepository.findAllByPlanIdOrderByStartTimeAsc(planId).stream()
            .map(shift -> toDetails(shift, plan.defaultStaffingMode()))
            .toList();
    }

    @Transactional
    public ShiftDetails update(UUID shiftId, ShiftData data) {
        ShiftEntity shift = requireShift(shiftId);
        planAccess.requirePlanner(shift.getPlanId());
        ShiftPlanDetails plan = shiftPlanService.get(shift.getPlanId());
        apply(data, shift, plan);
        return toDetails(shiftRepository.save(shift), plan.defaultStaffingMode());
    }

    public ShiftEntity requireShift(UUID shiftId) {
        return shiftRepository.findById(shiftId)
            .orElseThrow(() -> new ShiftNotFoundException("Shift not found: " + shiftId));
    }

    private void apply(ShiftData data, ShiftEntity shift, ShiftPlanDetails plan) {
        var event = eventCatalogQuery.requireEvent(plan.eventId());
        if (data.startTime().isBefore(event.startTime()) || data.endTime().isAfter(event.endTime())) {
            throw new ShiftValidationException("Shift time range must be within its event");
        }
        if (data.sessionId() != null) {
            var session = eventCatalogQuery.requireSession(data.sessionId());
            if (!plan.eventId().equals(session.eventId())) {
                throw new ShiftValidationException("Shift session must belong to the same event");
            }
        } else if (data.locationId() != null) {
            var location = eventCatalogQuery.requireLocation(data.locationId());
            if (!plan.eventId().equals(location.eventId())) {
                throw new ShiftValidationException("Shift location must belong to the same event");
            }
        }
        shift.setName(data.name());
        shift.setShortDescription(data.shortDescription());
        shift.setLongDescription(data.longDescription());
        shift.setStartTime(data.startTime());
        shift.setEndTime(data.endTime());
        shift.setLocationId(data.locationId());
        shift.setSessionId(data.sessionId());
        shift.setStaffingModeOverride(data.modeOverride().explicitMode());
    }

    private ShiftDetails toDetails(ShiftEntity shift, StaffingMode planMode) {
        UUID locationId = shift.getSessionId() == null
            ? shift.getLocationId()
            : eventCatalogQuery.requireSession(shift.getSessionId()).locationId();
        StaffingMode effectiveMode = shift.getStaffingModeOverride() == null ? planMode : shift.getStaffingModeOverride();
        return new ShiftDetails(shift.getId(), shift.getPlanId(), shift.getName(), shift.getShortDescription(),
            shift.getLongDescription(), shift.getStartTime(), shift.getEndTime(), locationId, shift.getSessionId(),
            ShiftModeOverride.from(shift.getStaffingModeOverride()), effectiveMode);
    }
}
