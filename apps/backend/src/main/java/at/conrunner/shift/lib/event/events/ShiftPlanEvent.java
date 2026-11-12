package at.conrunner.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.lib.event.BaseEvent;
import at.conrunner.shift.lib.event.EventType;
import at.conrunner.shift.lib.event.RoutingKeys;
import at.conrunner.shift.lib.event.events.parts.ShiftPlanPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShiftPlanEvent extends BaseEvent {
    private final ShiftPlanPart shiftPlan;

    public ShiftPlanEvent(EventType eventType, String routingKey, ShiftPlanPart shiftPlan) {
        super(eventType, routingKey);
        this.shiftPlan = shiftPlan;
    }

    public static ShiftPlanEvent ofInternal(EventType eventType, String routingKey, ShiftPlan shiftPlan) {
        return new ShiftPlanEvent(eventType, routingKey, ShiftPlanPart.of(shiftPlan));
    }

    public static ShiftPlanEvent planCreated(ShiftPlan shiftPlan) {
        return ofInternal(EventType.SHIFTPLAN_CREATED, RoutingKeys.SHIFTPLAN_CREATED, shiftPlan);
    }

    public static ShiftPlanEvent planUpdated(ShiftPlan shiftPlan) {
        return ofInternal(EventType.SHIFTPLAN_UPDATED,
            RoutingKeys.format(RoutingKeys.SHIFTPLAN_UPDATED,
            Map.of("shiftPlanId", String.valueOf(shiftPlan.getId()))), shiftPlan)
            .withDescription("Shift plan updated: " + shiftPlan.getName());
    }

    public static ShiftPlanEvent planDeleted(ShiftPlan shiftPlan) {
        return ofInternal(EventType.SHIFTPLAN_DELETED,
            RoutingKeys.format(RoutingKeys.SHIFTPLAN_DELETED,
            Map.of("shiftPlanId", String.valueOf(shiftPlan.getId()))), shiftPlan)
            .withDescription("Shift plan deleted: " + shiftPlan.getName());
    }

    public static ShiftPlanEvent planLockStatusChanged(ShiftPlan shiftPlan) {
        return ofInternal(EventType.SHIFTPLAN_LOCKSTATUS_CHANGED,
            RoutingKeys.format(RoutingKeys.SHIFTPLAN_LOCKSTATUS_CHANGED,
            Map.of("shiftPlanId", String.valueOf(shiftPlan.getId()))), shiftPlan)
            .withDescription("Shift plan lock status changed: " + shiftPlan.getName());
    }
}
