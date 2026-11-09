package at.control.shiftcontrol.shift.service.userprofile.impl;

import java.util.Set;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.exception.ForbiddenException;
import at.control.shiftcontrol.shift.lib.type.NotificationType;
import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.userprofile.NotificationSettingsDto;
import at.control.shiftcontrol.shift.dto.userprofile.UserProfileDto;
import at.control.shiftcontrol.shift.mapper.UserProfileMapper;
import at.control.shiftcontrol.shift.service.userprofile.NotificationService;
import at.control.shiftcontrol.shift.service.userprofile.UserProfileService;
import at.control.shiftcontrol.shift.service.userprofile.VolunteerCalendarService;
import at.control.shiftcontrol.shift.userdirectory.UserInfo;
import at.control.shiftcontrol.shift.userdirectory.UserLookupService;
import at.control.shiftcontrol.shift.userdirectory.current.CurrentUserProfileSyncService;
import at.control.shiftcontrol.shift.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class LocalUserProfileService implements UserProfileService {
    private final UserLookupService userDirectoryService;
    private final CurrentUserProfileSyncService currentUserProfileSyncService;
    private final NotificationService notificationService;
    private final CurrentShiftActor currentActor;
    private final SecurityHelper securityHelper;
    private final VolunteerDao volunteerDao;
    private final VolunteerCalendarService volunteerCalendarService;

    @Override
    public UserProfileDto getUserProfile(String userId) {
        var actor = currentActor.get();
        if (!userId.equals(actor.requireUserId().toString()) && securityHelper.isNotUserAdmin()) {
            throw new ForbiddenException("Access denied: cannot access other user's profile");
        }

        UserInfo user;
        if (userId.equals(actor.requireUserId().toString())) {
            user = currentUserProfileSyncService.syncCurrentSubjectIfStale();
        } else {
            user = userDirectoryService.getUserById(userId);
        }

        Volunteer volunteer = volunteerDao.findById(userId).orElse(null);
        String calendarShareId = userId.equals(actor.requireUserId().toString())
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
