package at.control.shiftcontrol.shift.service.userprofile.impl;

import java.util.UUID;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.platform.exception.ForbiddenException;
import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.userprofile.UserProfileDto;
import at.control.shiftcontrol.shift.mapper.UserProfileMapper;
import at.control.shiftcontrol.shift.service.userprofile.UserProfileService;
import at.control.shiftcontrol.shift.service.userprofile.VolunteerCalendarService;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserService;
import at.control.shiftcontrol.shift.util.SecurityHelper;

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
