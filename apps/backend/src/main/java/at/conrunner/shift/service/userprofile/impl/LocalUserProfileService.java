package at.conrunner.shift.service.userprofile.impl;

import java.util.UUID;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import at.conrunner.shift.entity.Volunteer;
import at.conrunner.platform.exception.ForbiddenException;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.userprofile.UserProfileDto;
import at.conrunner.shift.mapper.UserProfileMapper;
import at.conrunner.shift.service.userprofile.UserProfileService;
import at.conrunner.shift.service.userprofile.VolunteerCalendarService;
import at.conrunner.identity.User;
import at.conrunner.identity.UserService;
import at.conrunner.shift.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class LocalUserProfileService implements UserProfileService {
    private final UserService userService;
    private final CurrentShiftActor currentActor;
    private final SecurityHelper securityHelper;
    private final VolunteerDao volunteerDao;
    private final VolunteerCalendarService volunteerCalendarService;

    @Override
    public UserProfileDto getUserProfile(UUID userId) {
        var actor = currentActor.get();
        if (!userId.equals(actor.requireUserId()) && securityHelper.isNotUserAdmin()) {
            throw new ForbiddenException("Access denied: cannot access other user's profile");
        }

        User user = userService.getUser(userId);

        Volunteer volunteer = volunteerDao.findById(userId).orElse(null);
        String calendarShareId = userId.equals(actor.requireUserId())
            ? volunteerCalendarService.getOrCreateCalendarShareId(userId).orElse(null)
            : null;

        return UserProfileMapper.toUserProfileDto(user, volunteer, calendarShareId);
    }
}
