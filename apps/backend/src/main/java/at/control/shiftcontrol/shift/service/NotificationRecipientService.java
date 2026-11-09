package at.control.shiftcontrol.shift.service;

import at.control.shiftcontrol.identity.AdminOnly;
import at.control.shiftcontrol.shift.dto.notifications.RecipientsDto;
import at.control.shiftcontrol.shift.dto.notifications.RecipientsFilterDto;
import at.control.shiftcontrol.shift.dto.userprofile.AccountInfoDto;

import lombok.NonNull;

public interface NotificationRecipientService {
    @AdminOnly
    @NonNull
    AccountInfoDto getRecipientInformation(@NonNull String recipientid);

    @NonNull RecipientsDto getRecipientsForNotification(@NonNull RecipientsFilterDto filter);
}
