package at.control.shiftcontrol.shift.service.event.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.entity.Activity;
import at.shiftcontrol.lib.entity.Event;
import at.shiftcontrol.lib.entity.Location;
import at.shiftcontrol.lib.entity.PositionSlot;
import at.shiftcontrol.lib.entity.Role;
import at.shiftcontrol.lib.entity.Shift;
import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.event.events.EventEvent;
import at.shiftcontrol.lib.exception.IllegalStateException;
import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.dao.EventDao;
import at.shiftcontrol.shiftservice.dto.event.EventDto;
import at.shiftcontrol.shiftservice.mapper.EventMapper;
import at.shiftcontrol.shiftservice.service.event.EventCloneService;

@Service
@RequiredArgsConstructor
public class EventCloneServiceImpl implements EventCloneService {
    private final EventDao eventDao;
    private final ApplicationEventPublisher publisher;

    @Override
    @AdminOnly
    @Transactional
    public EventDto cloneEvent(long eventId) {
        Event source = eventDao.getById(eventId);

        CloneContext ctx = new CloneContext();

        Event target = cloneEventRoot(source);

        // clone independent children
        cloneLocations(source, target, ctx);
        cloneActivities(source, target, ctx);

        // clone shift plans and their internal hierarchy
        cloneShiftPlans(source, target, ctx);
        cloneRoles(source, ctx);
        cloneShifts(source, ctx);
        clonePositionSlots(source, ctx);

        // wire cross references using mappings
        wireShiftReferences(source, ctx);        // shift.location, shift.relatedActivity
        wireSlotReferences(source, ctx);         // slot.role

        // copy shared relations (volunteers) without cloning them
        copyShiftPlanVolunteers(source, ctx);

        // Persist everything once (cascades handle deep graph)
        Event persisted = eventDao.save(target);

        publisher.publishEvent(EventEvent.eventCloned(persisted, source.getId()));
        return EventMapper.toEventDto(persisted);
    }

    private Event cloneEventRoot(Event source) {
        return Event.builder()
            .name(source.getName() + " (Clone)")
            .shortDescription(source.getShortDescription())
            .longDescription(source.getLongDescription())
            .startTime(source.getStartTime())
            .endTime(source.getEndTime())
            .locations(new ArrayList<>())
            .shiftPlans(new ArrayList<>())
            .build();
    }

    private void cloneLocations(Event source, Event target, CloneContext ctx) {
        if (source.getLocations() == null) {
            return;
        }

        ensureEventLocationsInitialized(target);

        for (Location oldLoc : source.getLocations()) {
            Location newLoc = Location.builder()
                .event(target)
                .name(oldLoc.getName())
                .description(oldLoc.getDescription())
                .url(oldLoc.getUrl())
                .additionalProperties(oldLoc.getAdditionalProperties())
                .readOnly(oldLoc.isReadOnly())
                .build();

            target.getLocations().add(newLoc);
            ctx.locationByOldId.put(oldLoc.getId(), newLoc);
        }
    }

    private void ensureEventLocationsInitialized(Event target) {
        if (target.getLocations() == null) {
            target.setLocations(new ArrayList<>());
        }
    }

    private void cloneActivities(Event source, Event target, CloneContext ctx) {
        if (source.getActivities() == null) {
            return;
        }

        ensureEventActivitiesInitialized(target);

        for (Activity oldAct : source.getActivities()) {
            Location newLoc = null;
            if (oldAct.getLocation() != null) {
                newLoc = ctx.locationByOldId.get(oldAct.getLocation().getId());
            }

            Activity newAct = Activity.builder()
                .event(target)
                .name(oldAct.getName())
                .description(oldAct.getDescription())
                .startTime(oldAct.getStartTime())
                .endTime(oldAct.getEndTime())
                .location(newLoc)
                .readOnly(oldAct.isReadOnly())
                .build();

            target.getActivities().add(newAct);
            ctx.activityByOldId.put(oldAct.getId(), newAct);
        }
    }

    private void ensureEventActivitiesInitialized(Event target) {
        if (target.getActivities() == null) {
            target.setActivities(new ArrayList<>());
        }
    }

    private void cloneShiftPlans(Event source, Event target, CloneContext ctx) {
        if (source.getShiftPlans() == null) {
            return;
        }

        ensureEventShiftPlansInitialized(target);

        for (ShiftPlan oldPlan : source.getShiftPlans()) {
            ShiftPlan newPlan = ShiftPlan.builder()
                .event(target)
                .name(oldPlan.getName())
                .shortDescription(oldPlan.getShortDescription())
                .longDescription(oldPlan.getLongDescription())
                .lockStatus(oldPlan.getLockStatus())
                .defaultNoRolePointsPerMinute(oldPlan.getDefaultNoRolePointsPerMinute())
                .shifts(new ArrayList<>())
                .build();

            target.getShiftPlans().add(newPlan);
            ctx.shiftPlanByOldId.put(oldPlan.getId(), newPlan);
        }
    }

    private void ensureEventShiftPlansInitialized(Event target) {
        if (target.getShiftPlans() == null) {
            target.setShiftPlans(new ArrayList<>());
        }
    }

    private void cloneRoles(Event source, CloneContext ctx) {
        if (source.getShiftPlans() == null) {
            return;
        }

        for (ShiftPlan oldPlan : source.getShiftPlans()) {
            ShiftPlan newPlan = ctx.shiftPlanByOldId.get(oldPlan.getId());
            if (newPlan == null) {
                throw new IllegalStateException("Something went wrong during cloning shift plans");
            }

            if (oldPlan.getRoles() == null) {
                continue;
            }

            ensureShiftPlanRolesInitialized(newPlan);

            for (Role oldRole : oldPlan.getRoles()) {
                Role newRole = Role.builder()
                    .shiftPlan(newPlan)
                    .name(oldRole.getName())
                    .description(oldRole.getDescription())
                    .selfAssignable(oldRole.isSelfAssignable())
                    .rewardPointsPerMinute(oldRole.getRewardPointsPerMinute())
                    // volunteers wired later
                    .build();

                newPlan.getRoles().add(newRole);
                ctx.roleByOldId.put(oldRole.getId(), newRole);
            }
        }
    }

    private void ensureShiftPlanRolesInitialized(ShiftPlan plan) {
        if (plan.getRoles() == null) {
            plan.setRoles(new ArrayList<>());
        }
    }

    private void cloneShifts(Event source, CloneContext ctx) {
        if (source.getShiftPlans() == null) {
            return;
        }

        for (ShiftPlan oldPlan : source.getShiftPlans()) {
            ShiftPlan newPlan = ctx.shiftPlanByOldId.get(oldPlan.getId());
            if (newPlan == null) {
                throw new IllegalStateException("Something went wrong during cloning shift plans");
            }

            Collection<Shift> oldShifts = oldPlan.getShifts();
            if (oldShifts == null) {
                continue;
            }

            ensureShiftPlanShiftsInitialized(newPlan);

            for (Shift oldShift : oldShifts) {
                Shift newShift = Shift.builder()
                    .shiftPlan(newPlan)
                    .name(oldShift.getName())
                    .shortDescription(oldShift.getShortDescription())
                    .longDescription(oldShift.getLongDescription())
                    .startTime(oldShift.getStartTime())
                    .endTime(oldShift.getEndTime())
                    .bonusRewardPoints(oldShift.getBonusRewardPoints())
                    .slots(new ArrayList<>())
                    // location + relatedActivity wired later
                    .build();

                newPlan.getShifts().add(newShift);
                ctx.shiftByOldId.put(oldShift.getId(), newShift);
            }
        }
    }

    private void ensureShiftPlanShiftsInitialized(ShiftPlan plan) {
        if (plan.getShifts() == null) {
            plan.setShifts(new ArrayList<>());
        }
    }

    private void clonePositionSlots(Event source, CloneContext ctx) {
        if (source.getShiftPlans() == null) {
            return;
        }

        for (ShiftPlan oldPlan : source.getShiftPlans()) {
            Collection<Shift> oldShifts = oldPlan.getShifts();
            if (oldShifts == null) {
                continue;
            }

            for (Shift oldShift : oldShifts) {
                Shift newShift = ctx.shiftByOldId.get(oldShift.getId());
                if (newShift == null) {
                    throw new IllegalStateException("Something went wrong during cloning shifts");
                }

                Collection<PositionSlot> oldSlots = oldShift.getSlots();
                if (oldSlots == null) {
                    continue;
                }

                ensureShiftSlotsInitialized(newShift);

                for (PositionSlot oldSlot : oldSlots) {
                    PositionSlot newSlot = PositionSlot.builder()
                        .shift(newShift)
                        .name(oldSlot.getName())
                        .description(oldSlot.getDescription())
                        .skipAutoAssignment(oldSlot.isSkipAutoAssignment())
                        .desiredVolunteerCount(oldSlot.getDesiredVolunteerCount())
                        .overrideRewardPoints(oldSlot.getOverrideRewardPoints())
                        // role wired later
                        // assignments NOT copied
                        .build();

                    newShift.getSlots().add(newSlot);
                    ctx.slotByOldId.put(oldSlot.getId(), newSlot);
                }
            }
        }
    }

    private void ensureShiftSlotsInitialized(Shift shift) {
        if (shift.getSlots() == null) {
            shift.setSlots(new ArrayList<>());
        }
    }

    private void wireShiftReferences(Event source, CloneContext ctx) {
        if (source.getShiftPlans() == null) {
            return;
        }

        for (ShiftPlan oldPlan : source.getShiftPlans()) {
            Collection<Shift> oldShifts = oldPlan.getShifts();
            if (oldShifts == null) {
                continue;
            }

            for (Shift oldShift : oldShifts) {
                Shift newShift = ctx.shiftByOldId.get(oldShift.getId());
                if (newShift == null) {
                    throw new IllegalStateException("Something went wrong during cloning shifts");
                }

                // location
                if (oldShift.getLocation() != null) {
                    Location mappedLocation = ctx.locationByOldId.get(oldShift.getLocation().getId());
                    newShift.setLocation(mappedLocation);
                }

                // relatedActivity
                if (oldShift.getRelatedActivity() != null) {
                    Activity mappedActivity = ctx.activityByOldId.get(oldShift.getRelatedActivity().getId());
                    newShift.setRelatedActivity(mappedActivity);
                }
            }
        }
    }

    private void wireSlotReferences(Event source, CloneContext ctx) {
        if (source.getShiftPlans() == null) {
            return;
        }

        for (ShiftPlan oldPlan : source.getShiftPlans()) {
            Collection<Shift> oldShifts = oldPlan.getShifts();
            if (oldShifts == null) {
                continue;
            }

            for (Shift oldShift : oldShifts) {
                Collection<PositionSlot> oldSlots = oldShift.getSlots();
                if (oldSlots == null) {
                    continue;
                }

                for (PositionSlot oldSlot : oldSlots) {
                    PositionSlot newSlot = ctx.slotByOldId.get(oldSlot.getId());
                    if (newSlot == null) {
                        throw new IllegalStateException("Missing cloned slot for " + oldSlot.getId());
                    }

                    if (oldSlot.getRole() != null) {
                        Role mappedRole = ctx.roleByOldId.get(oldSlot.getRole().getId());
                        newSlot.setRole(mappedRole);
                    }
                }
            }
        }
    }

    private void copyShiftPlanVolunteers(Event source, CloneContext ctx) {
        if (source.getShiftPlans() == null) {
            return;
        }

        for (ShiftPlan oldPlan : source.getShiftPlans()) {
            ShiftPlan newPlan = ctx.shiftPlanByOldId.get(oldPlan.getId());
            if (newPlan == null) {
                throw new IllegalStateException("Something went wrong during cloning shift plans");
            }

            // Since Volunteer is owning side (JoinTable on Volunteer),
            // using addPlanVolunteer/addPlanPlanner is fine (updates both sides).
            if (oldPlan.getPlanVolunteers() != null) {
                for (Volunteer v : oldPlan.getPlanVolunteers()) {
                    newPlan.addPlanVolunteer(v);
                }
            }
            if (oldPlan.getPlanPlanners() != null) {
                for (Volunteer v : oldPlan.getPlanPlanners()) {
                    newPlan.addPlanPlanner(v);
                }
            }
        }
    }

    private static class CloneContext {
        public final Map<Long, Location> locationByOldId = new HashMap<>();
        public final Map<Long, Activity> activityByOldId = new HashMap<>();
        public final Map<Long, ShiftPlan> shiftPlanByOldId = new HashMap<>();
        public final Map<Long, Role> roleByOldId = new HashMap<>();
        public final Map<Long, Shift> shiftByOldId = new HashMap<>();
        public final Map<Long, PositionSlot> slotByOldId = new HashMap<>();
    }
}



