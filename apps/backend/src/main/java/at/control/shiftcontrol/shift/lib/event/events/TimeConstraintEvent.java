package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.control.shiftcontrol.shift.entity.TimeConstraint;
import at.control.shiftcontrol.shift.lib.event.BaseEvent;
import at.control.shiftcontrol.shift.lib.event.EventType;
import at.control.shiftcontrol.shift.lib.event.RoutingKeys;
import at.control.shiftcontrol.shift.lib.event.events.parts.TimeConstraintPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class TimeConstraintEvent extends BaseEvent {
    private final TimeConstraintPart timeConstraint;

    public TimeConstraintEvent(EventType eventType, String routingKey, TimeConstraintPart timeConstraint) {
        super(eventType, routingKey);
        this.timeConstraint = timeConstraint;
    }

    public static TimeConstraintEvent ofInternal(EventType eventType, String routingKey, TimeConstraint timeConstraint) {
        return new TimeConstraintEvent(eventType, routingKey, TimeConstraintPart.of(timeConstraint));
    }

    public static TimeConstraintEvent timeConstraintCreated(TimeConstraint timeConstraint) {
        return ofInternal(EventType.TIMECONSTRAINT_CREATED, RoutingKeys.TIMECONSTRAINT_CREATED, timeConstraint)
            .withDescription("New time constraint created for volunteer ID: " + timeConstraint.getVolunteer().getId());
    }

    public static TimeConstraintEvent timeConstraintDeleted(TimeConstraint timeConstraint) {
        return ofInternal(EventType.TIMECONSTRAINT_DELETED,
                RoutingKeys.format(RoutingKeys.TIMECONSTRAINT_DELETED, Map.of(
            "timeConstraintId", String.valueOf(timeConstraint.getId()),
            "volunteerId", timeConstraint.getVolunteer().getId())),
            timeConstraint)
            .withDescription("Time constraint deleted for volunteer ID: " + timeConstraint.getVolunteer().getId());
    }
}
