package at.conrunner.shift.lib.event.events;

import java.util.Collection;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.conrunner.shift.entity.Role;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.event.BaseEvent;
import at.conrunner.shift.lib.event.EventType;
import at.conrunner.shift.lib.event.RoutingKeys;
import at.conrunner.shift.lib.event.events.parts.RolePart;
import at.conrunner.shift.lib.event.events.parts.ShiftPlanRefPart;
import at.conrunner.shift.lib.event.events.parts.VolunteerPart;
import static at.conrunner.shift.lib.event.RoutingKeys.USERS_PLAN_BULK_ADD;
import static at.conrunner.shift.lib.event.RoutingKeys.USERS_PLAN_BULK_REMOVE;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPlanBulkEvent extends BaseEvent {
    private final Collection<VolunteerPart> volunteers;
    private final Collection<RolePart> roles;
    private final ShiftPlanRefPart shiftPlanRefPart;

    public UserPlanBulkEvent(EventType eventType, String routingKey,
                             Collection<VolunteerPart> volunteers, Collection<RolePart> roles, ShiftPlanRefPart shiftPlanRefPart) {
        super(eventType, routingKey);
        this.volunteers = volunteers;
        this.roles = roles;
        this.shiftPlanRefPart = shiftPlanRefPart;
    }

    public static UserPlanBulkEvent add(Collection<Volunteer> volunteers, Collection<Role> plans, ShiftPlan shiftPlan) {
        return new UserPlanBulkEvent(EventType.USERS_PLAN_BULK_ADD, RoutingKeys.format(USERS_PLAN_BULK_ADD, Map.of("shiftPlanId", shiftPlan.getId())),
            VolunteerPart.of(volunteers), RolePart.of(plans), ShiftPlanRefPart.of(shiftPlan))
            .withDescription("Added " + volunteers.size() + " volunteers to " + plans.size() + " plans in shift plan " + shiftPlan.getId());
    }

    public static UserPlanBulkEvent remove(Collection<Volunteer> volunteers, Collection<Role> plans, ShiftPlan shiftPlan) {
        return new UserPlanBulkEvent(EventType.USERS_PLAN_BULK_REMOVE, RoutingKeys.format(USERS_PLAN_BULK_REMOVE, Map.of("shiftPlanId", shiftPlan.getId())),
            VolunteerPart.of(volunteers), RolePart.of(plans), ShiftPlanRefPart.of(shiftPlan))
            .withDescription("Removed " + volunteers.size() + " volunteers from " + plans.size() + " plans in shift plan " + shiftPlan.getId());
    }
}
