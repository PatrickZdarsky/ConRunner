package at.control.shiftcontrol.shift.service.userprofile.impl;

import java.util.Set;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.exception.ForbiddenException;
import at.shiftcontrol.lib.type.NotificationType;
import at.shiftcontrol.shiftservice.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.dao.userprofile.VolunteerDao;
import at.shiftcontrol.shiftservice.dto.userprofile.NotificationSettingsDto;
import at.shiftcontrol.shiftservice.dto.userprofile.UserProfileDto;
import at.shiftcontrol.shiftservice.mapper.UserProfileMapper;
import at.shiftcontrol.shiftservice.service.userprofile.NotificationService;
import at.shiftcontrol.shiftservice.service.userprofile.UserProfileService;
import at.shiftcontrol.shiftservice.service.userprofile.VolunteerCalendarService;
import at.shiftcontrol.shiftservice.userdirectory.UserInfo;
import at.shiftcontrol.shiftservice.userdirectory.UserLookupService;
import at.shiftcontrol.shiftservice.userdirectory.current.CurrentUserProfileSyncService;
import at.shiftcontrol.shiftservice.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class LocalUserProfileService implements UserProfileService {
    private final UserLookupService userDirectoryService;
    private final CurrentUserProfileSyncService currentUserProfileSyncService;
    private final NotificationService notificationService;
    private final ApplicationUserProvider userProvider;
    private final SecurityHelper securityHelper;
    private final VolunteerDao volunteerDao;
    private final VolunteerCalendarService volunteerCalendarService;

    @Override
    public UserProfileDto getUserProfile(String userId) {
        var currentUser = userProvider.getCurrentUser();
        if (!userId.equals(currentUser.getUserId()) && securityHelper.isNotUserAdmin()) {
            throw new ForbiddenException("Access denied: cannot access other user's profile");
        }

        UserInfo user;
        if (userId.equals(currentUser.getUserId())) {
            user = currentUserProfileSyncService.syncCurrentSubjectIfStale();
        } else {
            user = userDirectoryService.getUserById(userId);
        }

        Volunteer volunteer = volunteerDao.findById(userId).orElse(null);
        String calendarShareId = userId.equals(currentUser.getUserId())
            ? volunteerCalendarService.getOrCreateCalendarShareId(userId).orElse(null)
            : null;

        /* fetch persistent notification settings and add empty set to missing types */
        var notifications = notificationService.getNotificationsForUser(userId);
        for (NotificationType type : NotificationType.values()) {
            if (notifications.stream().noneMatch(n -> n.getType() == type)) {
                notifications.add(NotificationSettingsDto
                    .builder()
                    .type(type)
                    .channels(Set.of())
                    .build()
                );
            }
        }

        return UserProfileMapper.toUserProfileDto(user, notifications, volunteer, calendarShareId);
    }
}
