package at.control.shiftcontrol.shift.service;

import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.dto.notifications.RecipientsDto;
import at.shiftcontrol.shiftservice.dto.notifications.RecipientsFilterDto;
import at.shiftcontrol.shiftservice.dto.userprofile.AccountInfoDto;

import lombok.NonNull;

public interface NotificationRecipientService {
    @AdminOnly
    @NonNull
    AccountInfoDto getRecipientInformation(@NonNull String recipientid);

    @NonNull RecipientsDto getRecipientsForNotification(@NonNull RecipientsFilterDto filter);
}
