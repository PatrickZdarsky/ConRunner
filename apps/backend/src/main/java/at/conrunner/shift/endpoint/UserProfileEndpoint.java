package at.conrunner.shift.endpoint;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.dto.userprofile.CalendarShareIdDto;
import at.conrunner.shift.dto.userprofile.UserProfileDto;
import at.conrunner.shift.service.userprofile.UserProfileService;
import at.conrunner.shift.service.userprofile.VolunteerCalendarService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/me", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserProfileEndpoint {
    private final CurrentShiftActor currentActor;
    private final UserProfileService userProfileService;
    private final VolunteerCalendarService volunteerCalendarService;

    @GetMapping("/profile")
    @Operation(
        operationId = "getCurrentUserProfile",
        description = "Get profile data of the current user"
    )
    public UserProfileDto getCurrentUserProfile() {
        return userProfileService.getUserProfile(currentActor.get().requireUserId());
    }

    @PostMapping("/profile/calendar-share-id/regenerate")
    @Operation(
        operationId = "regenerateCurrentUserCalendarShareId",
        description = "Regenerate the calendar share id of the current user"
    )
    public CalendarShareIdDto regenerateCalendarShareId() {
        return volunteerCalendarService.regenerateCalendarShareId(currentActor.get().requireUserId());
    }
}
