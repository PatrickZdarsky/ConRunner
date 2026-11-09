package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.NotificationSettings;
import at.shiftcontrol.lib.event.BaseEvent;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.lib.event.events.parts.NotificationSettingsPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationSettingsEvent extends BaseEvent {
    private final String volunteerId;
    private final NotificationSettingsPart notificationSettings;

    public NotificationSettingsEvent(EventType eventType, String routingKey, String volunteerId, NotificationSettingsPart notificationSettings) {
        super(eventType, routingKey);
        this.volunteerId = volunteerId;
        this.notificationSettings = notificationSettings;
    }

    public static NotificationSettingsEvent ofInternal(EventType eventType, String routingKey, String volunteerId, NotificationSettings notificationSettings) {
        return new NotificationSettingsEvent(eventType, routingKey, volunteerId, NotificationSettingsPart.of(notificationSettings));
    }

    public static NotificationSettingsEvent settingsUpdated(String volunteerId, NotificationSettings notificationSettings) {
        return ofInternal(EventType.VOLUNTEER_NOTIFICATION_PREFERENCE_UPDATED,
            RoutingKeys.format(RoutingKeys.VOLUNTEER_NOTIFICATION_PREFERENCE_UPDATED,
                Map.of("volunteerId", volunteerId)),
            volunteerId, notificationSettings).withDescription("Notification settings updated for volunteer ID: " + volunteerId);
    }
}
