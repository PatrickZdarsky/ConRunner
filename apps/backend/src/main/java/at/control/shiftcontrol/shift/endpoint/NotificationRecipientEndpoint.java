package at.control.shiftcontrol.shift.endpoint;

import at.control.shiftcontrol.shift.dto.notifications.RecipientsDto;

import at.control.shiftcontrol.shift.dto.notifications.RecipientsFilterDto;

import at.control.shiftcontrol.shift.dto.userprofile.AccountInfoDto;
import at.control.shiftcontrol.shift.service.NotificationRecipientService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.auth.ApplicationUserProvider;
import at.control.shiftcontrol.shift.dto.userprofile.NotificationSettingsDto;
import at.control.shiftcontrol.shift.dto.userprofile.UserProfileDto;
import at.control.shiftcontrol.shift.service.userprofile.NotificationService;
import at.control.shiftcontrol.shift.service.userprofile.UserProfileService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/me", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class NotificationRecipientEndpoint {
    private final NotificationRecipientService notificationRecipientService;

    @GetMapping("/recipients")
    @Operation(
        operationId = "getRecipientsForNotification",
        description = "Get the recipients for a specific notification based on the provided filter"
    )
    public RecipientsDto getRecipientsForNotification(@RequestBody @Valid RecipientsFilterDto filter) {
        return notificationRecipientService.getRecipientsForNotification(filter);
    }


    @GetMapping("/recipients/{recipientId}")
    @Operation(
        operationId = "getRecipientInformation",
        description = "Get information about a specific recipient"
    )
    public AccountInfoDto getRecipientInformation(@PathVariable String recipientId) {
        return notificationRecipientService.getRecipientInformation(recipientId);
    }
}
