package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.lib.event.events.parts.ShiftPlanPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShiftPlanVolunteerEvent extends ShiftPlanEvent {
    private final String volunteerId;

    public ShiftPlanVolunteerEvent(EventType eventType, String routingKey, ShiftPlanPart shiftPlan, String volunteerId) {
        super(eventType, routingKey, shiftPlan);
        this.volunteerId = volunteerId;
    }

    public static ShiftPlanVolunteerEvent ofInternal(EventType eventType, String routingKey, ShiftPlan shiftPlan, String volunteerId) {
        return new ShiftPlanVolunteerEvent(eventType, routingKey, ShiftPlanPart.of(shiftPlan), volunteerId);
    }

    public static ShiftPlanVolunteerEvent joinedAsVolunteer(ShiftPlan shiftPlan, String volunteerId) {
        return ofInternal(EventType.SHIFTPLAN_JOINED_VOLUNTEER,
            RoutingKeys.format(RoutingKeys.SHIFTPLAN_JOINED_VOLUNTEER,
            Map.of("shiftPlanId", String.valueOf(shiftPlan.getId()),
                "volunteerId", volunteerId)), shiftPlan, volunteerId)
            .withDescription("Volunteer " + volunteerId + " joined shift plan " + shiftPlan.getId());
    }

    public static ShiftPlanVolunteerEvent joinedAsPlanner(ShiftPlan shiftPlan, String volunteerId) {
        return ofInternal(EventType.SHIFTPLAN_JOINED_PLANNER,
            RoutingKeys.format(RoutingKeys.SHIFTPLAN_JOINED_PLANNER,
            Map.of("shiftPlanId", String.valueOf(shiftPlan.getId()),
                "volunteerId", volunteerId)), shiftPlan, volunteerId)
            .withDescription("Planner " + volunteerId + " joined shift plan " + shiftPlan.getId());
    }
}
