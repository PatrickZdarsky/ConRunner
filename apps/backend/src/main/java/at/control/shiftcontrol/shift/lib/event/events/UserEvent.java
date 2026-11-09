package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.event.BaseEvent;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.lib.event.events.parts.ShiftPlanRefPart;
import at.shiftcontrol.lib.event.events.parts.VolunteerPart;
import static at.shiftcontrol.lib.event.RoutingKeys.USERS_EVENT_LOCK;
import static at.shiftcontrol.lib.event.RoutingKeys.USERS_EVENT_UNLOCK;
import static at.shiftcontrol.lib.event.RoutingKeys.USERS_EVENT_UPDATE;
import static at.shiftcontrol.lib.event.RoutingKeys.USERS_PLAN_UPDATE;
import static at.shiftcontrol.lib.event.RoutingKeys.USERS_RESET;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserEvent extends BaseEvent {
    private final VolunteerPart volunteer;
    private final Collection<ShiftPlanRefPart> shiftPlanRefParts;

    public UserEvent(EventType eventType, String routingKey, VolunteerPart volunteer, Collection<ShiftPlanRefPart> shiftPlanRefParts) {
        super(eventType, routingKey);
        this.volunteer = volunteer;
        this.shiftPlanRefParts = shiftPlanRefParts;
    }

    public static UserEvent eventUpdate(Volunteer volunteer, Collection<ShiftPlan> plans) {
        return new UserEvent(EventType.USERS_EVENT_UPDATE,
            RoutingKeys.format(USERS_EVENT_UPDATE, Map.of("userId", volunteer.getId())), VolunteerPart.of(volunteer), ShiftPlanRefPart.of(plans))
            .withDescription("User event updated for volunteer ID: " + volunteer.getId());
    }

    public static UserEvent planUpdate(Volunteer volunteer, ShiftPlan plan) {
        return new UserEvent(EventType.USERS_PLAN_UPDATE,
            RoutingKeys.format(USERS_PLAN_UPDATE,
            Map.of("shiftPlanId", plan.getId(), "userId", volunteer.getId())),
            VolunteerPart.of(volunteer), ShiftPlanRefPart.of(List.of(plan)))
            .withDescription("User plan updated for volunteer ID: " + volunteer.getId() + " in shift plan ID: " + plan.getId());
    }

    public static UserEvent lock(Volunteer volunteer, Collection<ShiftPlan> plans) {
        return new UserEvent(EventType.USERS_EVENT_LOCK,
            RoutingKeys.format(USERS_EVENT_LOCK, Map.of("userId", volunteer.getId())), VolunteerPart.of(volunteer), ShiftPlanRefPart.of(plans))
            .withDescription("User locked for volunteer ID: " + volunteer.getId());
    }

    public static UserEvent unlock(Volunteer volunteer, Collection<ShiftPlan> plans) {
        return new UserEvent(EventType.USERS_EVENT_UNLOCK,
            RoutingKeys.format(USERS_EVENT_UNLOCK, Map.of("userId", volunteer.getId())), VolunteerPart.of(volunteer), ShiftPlanRefPart.of(plans))
            .withDescription("User unlocked for volunteer ID: " + volunteer.getId());
    }

    public static UserEvent reset(Volunteer volunteer, Collection<ShiftPlan> plans) {
        return new UserEvent(EventType.USERS_RESET,
            RoutingKeys.format(USERS_RESET, Map.of("userId", volunteer.getId())), VolunteerPart.of(volunteer), ShiftPlanRefPart.of(plans))
            .withDescription("User reset for volunteer ID: " + volunteer.getId());
    }
}
