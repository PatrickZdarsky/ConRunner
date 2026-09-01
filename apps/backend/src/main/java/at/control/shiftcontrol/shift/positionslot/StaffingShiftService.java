package at.control.shiftcontrol.shift.positionslot;

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
public class StaffingShiftService {
    private final StaffingShiftRepository staffingShiftRepository;
    private final ShiftPlanService shiftPlanService;
    private final PlanAccess planAccess;
    private final EventCatalogQuery eventCatalogQuery;
    private final Clock clock;

    @Transactional
    public StaffingShiftDetails create(UUID planId, StaffingShiftData data) {
        planAccess.requirePlanner(planId);
        ShiftPlanDetails plan = shiftPlanService.get(planId);
        StaffingShiftEntity shift = new StaffingShiftEntity();
        shift.setPlanId(planId);
        shift.setCreatedAt(clock.instant());
        apply(data, shift, plan);
        return toDetails(staffingShiftRepository.save(shift), plan.defaultStaffingMode());
    }

    public StaffingShiftDetails get(UUID staffingShiftId) {
        StaffingShiftEntity shift = requireShift(staffingShiftId);
        planAccess.requireMember(shift.getPlanId());
        return toDetails(shift, shiftPlanService.get(shift.getPlanId()).defaultStaffingMode());
    }

    public Collection<StaffingShiftDetails> listForPlan(UUID planId) {
        planAccess.requirePlanner(planId);
        ShiftPlanDetails plan = shiftPlanService.get(planId);
        return staffingShiftRepository.findAllByPlanIdOrderByStartTimeAsc(planId).stream()
            .map(shift -> toDetails(shift, plan.defaultStaffingMode()))
            .toList();
    }

    @Transactional
    public StaffingShiftDetails update(UUID staffingShiftId, StaffingShiftData data) {
        StaffingShiftEntity shift = requireShift(staffingShiftId);
        planAccess.requirePlanner(shift.getPlanId());
        ShiftPlanDetails plan = shiftPlanService.get(shift.getPlanId());
        apply(data, shift, plan);
        return toDetails(staffingShiftRepository.save(shift), plan.defaultStaffingMode());
    }

    StaffingShiftEntity requireShift(UUID staffingShiftId) {
        return staffingShiftRepository.findById(staffingShiftId)
            .orElseThrow(() -> new ShiftNotFoundException("Staffing shift not found: " + staffingShiftId));
    }

    private void apply(StaffingShiftData data, StaffingShiftEntity shift, ShiftPlanDetails plan) {
        var event = eventCatalogQuery.requireEvent(plan.eventId());
        if (data.startTime().isBefore(event.startTime()) || data.endTime().isAfter(event.endTime())) {
            throw new ShiftValidationException("Staffing shift time range must be within its event");
        }
        if (data.sessionId() != null) {
            var session = eventCatalogQuery.requireSession(data.sessionId());
            if (!plan.eventId().equals(session.eventId())) {
                throw new ShiftValidationException("Staffing shift session must belong to the same event");
            }
        } else if (data.locationId() != null) {
            var location = eventCatalogQuery.requireLocation(data.locationId());
            if (!plan.eventId().equals(location.eventId())) {
                throw new ShiftValidationException("Staffing shift location must belong to the same event");
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

    private StaffingShiftDetails toDetails(StaffingShiftEntity shift, StaffingMode planMode) {
        UUID locationId = shift.getSessionId() == null
            ? shift.getLocationId()
            : eventCatalogQuery.requireSession(shift.getSessionId()).locationId();
        StaffingMode effectiveMode = shift.getStaffingModeOverride() == null
            ? planMode
            : shift.getStaffingModeOverride();
        return new StaffingShiftDetails(shift.getId(), shift.getPlanId(), shift.getName(), shift.getShortDescription(),
            shift.getLongDescription(), shift.getStartTime(), shift.getEndTime(), locationId, shift.getSessionId(),
            ShiftModeOverride.from(shift.getStaffingModeOverride()), effectiveMode);
    }
}
