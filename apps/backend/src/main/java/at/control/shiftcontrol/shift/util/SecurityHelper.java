package at.control.shiftcontrol.shift.util;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.platform.exception.ForbiddenException;
import at.control.shiftcontrol.platform.exception.IllegalArgumentException;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.auth.ShiftActor;
import at.control.shiftcontrol.shift.dao.EventDao;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityHelper {
    private final CurrentShiftActor currentActor;
    private final EventDao eventDao;

    public boolean isUserPlanner(long shiftPlanId, ShiftActor actor) {
        return actor.isPlannerInPlan(shiftPlanId);
    }

    public boolean isUserPlanner(long shiftPlanId) {
        var actor = currentActor.get();
        return isUserPlanner(shiftPlanId, actor);
    }

    public void assertUserIsPlanner(long shiftPlanId) {
        assertUserIsPlanner(shiftPlanId, currentActor.get());
    }

    public void assertUserIsPlanner(long shiftPlanId, ShiftActor actor) {
        if (!isUserPlanner(shiftPlanId, actor)) {
            log.error("User is not a planner in shift plan with id: {}", shiftPlanId);
            throw new ForbiddenException("User is not a planner in shift plan.");
        }
    }

    public boolean isUserPlannerInAnyPlanOfEvent(Event event) {
        if (event.getShiftPlans() == null || event.getShiftPlans().isEmpty()) {
            return false;
        }

        return event.getShiftPlans()
            .stream()
            .anyMatch(shiftPlan -> isUserPlanner(shiftPlan.getId()));
    }

    public void assertUserIsPlanner(ShiftPlan shiftPlan) {
        assertUserIsPlanner(shiftPlan.getId());
    }

    public void assertUserIsPlanner(Shift shift) {
        assertUserIsPlanner(shift.getShiftPlan());
    }

    public void assertUserIsPlanner(PositionSlot positionSlot) {
        assertUserIsPlanner(positionSlot.getShift());
    }

    public void assertUserIsPlannerInAnyPlanOfEvent(Event event) {
        boolean isPlannerInAnyPlan = isUserPlannerInAnyPlanOfEvent(event);
        var isNotAdmin = isNotUserAdmin();
        if (!isPlannerInAnyPlan && isNotAdmin) {
            log.error("User has no planner access to any shift plan of event with id: {}", event.getId());
            throw new ForbiddenException("User has no planner access to any shift plan of event.");
        }
    }

    public void assertUserIsPlannerInAnyPlanOfEvent(String eventId) {
        Event event = eventDao.getById(ConvertUtil.idToLong(eventId));
        assertUserIsPlannerInAnyPlanOfEvent(event);
    }
    //     --------------------- Volunteer ---------------------

    public boolean isVolunteerInPlan(long shiftPlanId, ShiftActor actor) {
        return actor.isVolunteerInPlan(shiftPlanId);
    }

    public boolean isVolunteerInPlan(long shiftPlanId) {
        var actor = currentActor.get();
        return isVolunteerInPlan(shiftPlanId, actor);
    }

    public boolean isVolunteerLockedInPlan(long shiftPlanId, ShiftActor actor) {
        return actor.isLockedInPlan(shiftPlanId);
    }

    public boolean isVolunteerLockedInPlan(ShiftPlan plan, Volunteer volunteer) {
        return volunteer.getLockedPlans().contains(plan);
    }

    public boolean isUserOnlyVolunteerInPlan(long shiftPlanId) {
        var actor = currentActor.get();
        return isVolunteerInPlan(shiftPlanId, actor) && !isUserPlanner(shiftPlanId, actor);
    }

    public void assertUserIsVolunteer(long shiftPlanId, ShiftActor actor, boolean needsWriteAccess) {
        if (!isVolunteerInPlan(shiftPlanId, actor)) {
            log.error("User is not a volunteer in plan with id: {}", shiftPlanId);
            throw new ForbiddenException("User is not a volunteer in plan.");
        }
        if (needsWriteAccess && isVolunteerLockedInPlan(shiftPlanId, actor)) {
            log.error("User is locked in plan with id: {}", shiftPlanId);
            throw new ForbiddenException("User is locked in plan.");
        }
    }

    public void assertUserIsVolunteer(long shiftPlanId, boolean needsWriteAccess) {
        assertUserIsVolunteer(shiftPlanId, currentActor.get(), needsWriteAccess);
    }

    public void assertUserIsVolunteer(ShiftPlan shiftPlan, boolean needsWriteAccess) {
        assertUserIsVolunteer(shiftPlan.getId(), needsWriteAccess);
    }

    public void assertUserIsVolunteer(Shift shift, boolean needsWriteAccess) {
        assertUserIsVolunteer(shift.getShiftPlan(), needsWriteAccess);
    }

    public void assertUserIsVolunteer(PositionSlot positionSlot, boolean needsWriteAccess) {
        assertUserIsVolunteer(positionSlot.getShift(), needsWriteAccess);
    }

    public void assertVolunteerIsVolunteer(ShiftPlan plan, Volunteer volunteer, boolean needsWriteAccess) {
        if (!volunteer.getVolunteeringPlans().contains(plan)) {
            throw new IllegalArgumentException("User not contained in plan.");
        }
        if (needsWriteAccess && isVolunteerLockedInPlan(plan, volunteer)) {
            log.error("User is locked in plan with id: {}", plan.getId());
            throw new ForbiddenException("User is locked in plan.");
        }
    }

    public void assertVolunteerIsVolunteer(Shift shift, Volunteer volunteer, boolean needsWriteAccess) {
        assertVolunteerIsVolunteer(shift.getShiftPlan(), volunteer, needsWriteAccess);
    }

    public void assertVolunteerIsVolunteer(PositionSlot slot, Volunteer volunteer, boolean needsWriteAccess) {
        assertVolunteerIsVolunteer(slot.getShift(), volunteer, needsWriteAccess);
    }

    public void assertVolunteerIsLockedInPlan(ShiftPlan plan, Volunteer volunteer) {
        assertVolunteerIsVolunteer(plan, volunteer, false);
        if (!isVolunteerLockedInPlan(plan, volunteer)) {
            throw new ForbiddenException("User is already not locked");
        }
    }

    public void assertVolunteerIsNotLockedInPlan(ShiftPlan plan, Volunteer volunteer) {
        assertVolunteerIsVolunteer(plan, volunteer, false);
        if (isVolunteerLockedInPlan(plan, volunteer)) {
            throw new ForbiddenException("User is already locked");
        }
    }

    public void assertVolunteerIsNotLockedInPlan(Assignment ass) {
        assertVolunteerIsVolunteer(ass.getPositionSlot(), ass.getAssignedVolunteer(), true);
        if (isVolunteerLockedInPlan(ass.getPositionSlot().getShift().getShiftPlan(), ass.getAssignedVolunteer())) {
            throw new ForbiddenException("User is already locked");
        }
    }
    //     --------------------- Volunteer or Planner ---------------------

    private boolean isUserInPlan(long shiftPlanId) {
        var actor = currentActor.get();
        return actor.isVolunteerInPlan(shiftPlanId) || actor.isPlannerInPlan(shiftPlanId);
    }

    public boolean isUserInPlan(Shift shift) {
        return isUserInPlan(shift.getShiftPlan().getId());
    }

    public void assertUserIsInPlan(long shiftPlanId) {
        if (!isUserInPlan(shiftPlanId)) {
            log.error("User has no access to shift plan with id: {}", shiftPlanId);
            throw new ForbiddenException("User has no access to shift plan.");
        }
    }

    public void assertUserIsInPlan(ShiftPlan shiftPlan) {
        assertUserIsInPlan(shiftPlan.getId());
    }

    public void assertUserIsInPlan(Shift shift) {
        assertUserIsInPlan(shift.getShiftPlan());
    }

    public void assertUserIsInPlan(PositionSlot positionSlot) {
        assertUserIsInPlan(positionSlot.getShift());
    }

    private boolean isUserInAnyPlanOfEvent(Event event) {
        return event.getShiftPlans()
            .stream()
            .anyMatch(shiftPlan -> isUserInPlan(shiftPlan.getId()));
    }

    public void assertUserIsAllowedToAccessEvent(Event event) {
        boolean isInAnyPlan = isUserInAnyPlanOfEvent(event); // also false if no shift plans exist
        boolean isNotAdmin = isNotUserAdmin();
        if (!isInAnyPlan && isNotAdmin) {
            log.error("User has no access to any shift plan of event with id: {}", event.getId());
            throw new ForbiddenException("User has no access to any shift plan of event.");
        }
    }

    //     --------------------- Admin ---------------------
    public boolean isUserAdmin() {
        var actor = currentActor.get();
        return isUserAdmin(actor);
    }

    public boolean isUserAdmin(ShiftActor actor) {
        return actor.isPlatformAdmin()
            || actor.getActor().authorities().stream().anyMatch(authority -> "ADMIN".equals(authority.getAuthority()));
    }

    public boolean isNotUserAdmin() {
        return !isUserAdmin();
    }

    public boolean isNotUserAdmin(ShiftActor actor) {
        return !isUserAdmin(actor);
    }
}
