package at.conrunner.shift.lib.event.events;

import java.util.Collection;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.event.BaseEvent;
import at.conrunner.shift.lib.event.EventType;
import at.conrunner.shift.lib.event.events.parts.ShiftPlanPart;
import at.conrunner.shift.lib.event.events.parts.VolunteerPart;
import static at.conrunner.shift.lib.event.RoutingKeys.USERS_EVENT_BULK_ADD;
import static at.conrunner.shift.lib.event.RoutingKeys.USERS_EVENT_BULK_REMOVE;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserEventBulkEvent extends BaseEvent {
    private final Collection<VolunteerPart> volunteers;
    private final Collection<ShiftPlanPart> plans;

    public UserEventBulkEvent(EventType eventType, String routingKey, Collection<VolunteerPart> volunteers, Collection<ShiftPlanPart> plans) {
        super(eventType, routingKey);
        this.volunteers = volunteers;
        this.plans = plans;
    }

    public static UserEventBulkEvent add(Collection<Volunteer> volunteers, Collection<ShiftPlan> plans) {
        return new UserEventBulkEvent(EventType.USERS_EVENT_BULK_ADD, USERS_EVENT_BULK_ADD, VolunteerPart.of(volunteers), ShiftPlanPart.of(plans))
            .withDescription("Added " + volunteers.size() + " volunteers to " + plans.size() + " shift plans.");
    }

    public static UserEventBulkEvent remove(Collection<Volunteer> volunteers, Collection<ShiftPlan> plans) {
        return new UserEventBulkEvent(EventType.USERS_EVENT_BULK_REMOVE, USERS_EVENT_BULK_REMOVE, VolunteerPart.of(volunteers), ShiftPlanPart.of(plans))
            .withDescription("Removed " + volunteers.size() + " volunteers from " + plans.size() + " shift plans.");
    }
}
