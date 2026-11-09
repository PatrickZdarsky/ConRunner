package at.control.shiftcontrol.shift.endpoint;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.shiftservice.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.dto.userprofile.CalendarShareIdDto;
import at.shiftcontrol.shiftservice.dto.userprofile.NotificationSettingsDto;
import at.shiftcontrol.shiftservice.dto.userprofile.UserProfileDto;
import at.shiftcontrol.shiftservice.service.userprofile.NotificationService;
import at.shiftcontrol.shiftservice.service.userprofile.UserProfileService;
import at.shiftcontrol.shiftservice.service.userprofile.VolunteerCalendarService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/me", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserProfileEndpoint {
    private final ApplicationUserProvider userProvider;
    private final UserProfileService userProfileService;
    private final NotificationService notificationService;
    private final VolunteerCalendarService volunteerCalendarService;

    @GetMapping("/profile")
    @Operation(
        operationId = "getCurrentUserProfile",
        description = "Get profile data of the current user (account, notifications, unavailability)"
    )
    public UserProfileDto getCurrentUserProfile() {
        return userProfileService.getUserProfile(userProvider.getCurrentUser().getUserId());
    }

    @PatchMapping("/profile/notifications")
    @Operation(
        operationId = "updateNotificationSettings",
        description = "Update notification settings of the current user"
    )
    public NotificationSettingsDto updateNotificationSettings(@RequestBody @Valid NotificationSettingsDto updateDto) {
        return notificationService.updateNotificationSetting(userProvider.getCurrentUser().getUserId(), updateDto);
    }

    @PostMapping("/profile/calendar-share-id/regenerate")
    @Operation(
        operationId = "regenerateCurrentUserCalendarShareId",
        description = "Regenerate the calendar share id of the current user"
    )
    public CalendarShareIdDto regenerateCalendarShareId() {
        return volunteerCalendarService.regenerateCalendarShareId(userProvider.getCurrentUser().getUserId());
    }
}
