package at.control.shiftcontrol.shift.service.userprofile;

import java.util.Collection;

import at.shiftcontrol.shiftservice.dto.userprofile.NotificationSettingsDto;

public interface NotificationService {
    Collection<NotificationSettingsDto> getNotificationsForUser(String userId);

    NotificationSettingsDto updateNotificationSetting(String userId, NotificationSettingsDto settingsDto);
}
