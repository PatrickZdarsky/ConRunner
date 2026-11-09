package at.control.shiftcontrol.shift.dao;

import java.util.Collection;

import at.shiftcontrol.lib.entity.NotificationSettings;
import at.shiftcontrol.lib.type.NotificationChannel;
import at.shiftcontrol.lib.type.NotificationType;

public interface NotificationSettingsDao extends BasicDao<NotificationSettings, Long> {
    Collection<NotificationSettings> findAllByUserId(String userId);

    Collection<String> findAllByNotificationTypeAndChannel(
        NotificationType notificationType, NotificationChannel notificationChannel);

    Collection<String> findAllByNotificationTypeAndChannelDisabled(
        NotificationType notificationType, NotificationChannel notificationChannel, Collection<String> userIds);

    Collection<String> findAllByNotificationTypeAndChannelAndUserIds(
        NotificationType notificationType, NotificationChannel notificationChannel, Collection<String> userIds);
}
