package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import at.control.shiftcontrol.shift.entity.Activity;
import at.control.shiftcontrol.shift.lib.event.BaseEvent;
import at.control.shiftcontrol.shift.lib.event.EventType;
import at.control.shiftcontrol.shift.lib.event.RoutingKeys;
import at.control.shiftcontrol.shift.lib.event.events.parts.ActivityPart;

@EqualsAndHashCode(callSuper = true)
@Data
public class ActivityEvent extends BaseEvent {
    @NotNull
    private final ActivityPart activity;

    public ActivityEvent(EventType eventType, String routingKey, ActivityPart activity) {
        super(eventType, routingKey);
        this.activity = activity;
    }

    public static ActivityEvent ofInternal(EventType eventType, String routingKey, Activity activity) {
        return new ActivityEvent(eventType, routingKey, ActivityPart.of(activity));
    }

    public static ActivityEvent activityCreated(Activity activity) {
        return ofInternal(EventType.ACTIVITY_CREATED, RoutingKeys.ACTIVITY_CREATED, activity)
            .withDescription("New activity created: " + activity.getName());
    }

    public static ActivityEvent activityUpdated(Activity activity) {
        return ofInternal(EventType.ACTIVITY_UPDATED,
            RoutingKeys.format(RoutingKeys.ACTIVITY_UPDATED,
            Map.of("activityId", String.valueOf(activity.getId()))), activity)
            .withDescription("Activity updated: " + activity.getName());
    }

    public static ActivityEvent activityDeleted(Activity activity) {
        return ofInternal(EventType.ACTIVITY_DELETED,
            RoutingKeys.format(RoutingKeys.ACTIVITY_DELETED,
            Map.of("activityId", String.valueOf(activity.getId()))), activity)
            .withDescription("Activity deleted: " + activity.getName());
    }
}
