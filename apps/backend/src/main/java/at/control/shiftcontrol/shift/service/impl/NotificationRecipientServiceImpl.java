package at.control.shiftcontrol.shift.service.impl;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.jspecify.annotations.NonNull;

import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.platform.exception.BadRequestException;
import at.control.shiftcontrol.shift.lib.type.NotificationChannel;
import at.control.shiftcontrol.shift.lib.type.ReceiverAccessLevel;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.identity.AdminOnly;
import at.control.shiftcontrol.shift.dao.NotificationSettingsDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.notifications.RecipientsDto;
import at.control.shiftcontrol.shift.dto.notifications.RecipientsFilterDto;
import at.control.shiftcontrol.shift.dto.userprofile.AccountInfoDto;
import at.control.shiftcontrol.shift.mapper.AccountInfoMapper;
import at.control.shiftcontrol.shift.service.NotificationRecipientService;
import at.control.shiftcontrol.shift.userdirectory.UserInfo;
import at.control.shiftcontrol.shift.userdirectory.UserLookupService;

@Service
@RequiredArgsConstructor
public class NotificationRecipientServiceImpl implements NotificationRecipientService {
    private final VolunteerDao volunteerDao;
    private final UserLookupService userDirectoryService;
    private final NotificationSettingsDao notificationSettingsDao;

    @AdminOnly
    @Override
    public @NonNull AccountInfoDto getRecipientInformation(@NonNull String recipientid) {
        var user = userDirectoryService.getUserById(recipientid);
        return AccountInfoMapper.toDto(user);
    }

    @Override
    @AdminOnly
    public @NonNull RecipientsDto getRecipientsForNotification(@NonNull RecipientsFilterDto filter) {
        validateFilter(filter);
        // filter from cheapest and most narrowing query first
        Collection<String> recipientIds = null;

        /* if not only admins are target, filter by event/plan/volunteer specs*/
        if (filter.getReceiverAccessLevel() != ReceiverAccessLevel.ADMIN){

            // filter by eventId, shiftId and volunteerIds if present
            recipientIds = filterByShiftPlanAndVolunteerIds(filter, recipientIds);
            recipientIds = filterByEventAndVolunteerIds(filter, recipientIds);
            recipientIds = filterByVolunteerIds(filter, recipientIds);
        }

        /* if target level is admin or planner (admin equals a planner matching all filters), append all admins to recipients */
        if (filter.getReceiverAccessLevel() == ReceiverAccessLevel.ADMIN || filter.getReceiverAccessLevel() == ReceiverAccessLevel.PLANNER) {
            var adminIds = filterAdminsById(filter);
            if (recipientIds != null) {
                recipientIds.addAll(adminIds);
            } else {
                recipientIds = adminIds;
            }
        }

        if (recipientIds != null) {
            if (filter.getNotificationChannel() == NotificationChannel.EMAIL) {
                // EMAIL default OFF --> include only explicit settings
                recipientIds = notificationSettingsDao.findAllByNotificationTypeAndChannelAndUserIds(
                        filter.getNotificationType(),
                        NotificationChannel.EMAIL,
                        recipientIds
                    );
            } else {
                // other channels are default ON --> remove those who have the type and channel DISABLED
                Collection<String> nonRecipients = notificationSettingsDao.findAllByNotificationTypeAndChannelDisabled(
                    filter.getNotificationType(),
                    filter.getNotificationChannel(),
                    recipientIds);
                recipientIds.removeAll(nonRecipients);
            }
        } else {
            // else get all that have the notification type and channel enabled
                // this should never be called imo, since all planner & volunteer events always should have a filter enabled
                // only admin notifications should have no filter, but that's handled at the top
            recipientIds = notificationSettingsDao.findAllByNotificationTypeAndChannel(
                filter.getNotificationType(),
                filter.getNotificationChannel()
            );
        }

        // get users and build response
        if (recipientIds.isEmpty()) {
            return RecipientsDto.builder().recipients(Set.of()).build();
        } else {
            var users = userDirectoryService.getUserByIds(recipientIds);
            var recipients = users.stream().map(AccountInfoMapper::toDto).collect(Collectors.toSet());
            return RecipientsDto.builder()
                .recipients(recipients)
                .build();
        }
    }

    private Collection<String> filterByEventAndVolunteerIds(RecipientsFilterDto filter, Collection<String> recipientIds) {
        if (filter.getRelatedEventId() == null) {
            return recipientIds;
        }
        if (filter.getRelatedVolunteerIds() != null) {
            return (switch (filter.getReceiverAccessLevel()) {
                case VOLUNTEER -> volunteerDao.findAllByEventAndVolunteerIds(
                    ConvertUtil.idToLong(filter.getRelatedEventId()),
                    filter.getRelatedVolunteerIds()
                );
                case PLANNER -> volunteerDao.findAllByEventAndPlannerIds(
                    ConvertUtil.idToLong(filter.getRelatedEventId()),
                    filter.getRelatedVolunteerIds()
                );
                case ADMIN -> throw new NotImplementedException(); // should be handled at root
            }).stream().map(Volunteer::getId).collect(Collectors.toSet());
        } else {
            return (switch (filter.getReceiverAccessLevel()) {
                case VOLUNTEER -> volunteerDao.findAllByEvent(
                    ConvertUtil.idToLong(filter.getRelatedEventId())
                );
                case PLANNER -> volunteerDao.findAllPlannersByEvent(
                    ConvertUtil.idToLong(filter.getRelatedEventId())
                );
                case ADMIN -> throw new NotImplementedException(); // should be handled at root
            }).stream().map(Volunteer::getId).collect(Collectors.toSet());
        }
    }

    private Collection<String> filterByShiftPlanAndVolunteerIds(RecipientsFilterDto filter, Collection<String> recipientIds) {
        if (filter.getRelatedShiftPlanId() == null) {
            return recipientIds;
        }
        if (filter.getRelatedVolunteerIds() != null) {
            return (switch (filter.getReceiverAccessLevel()) {
                case VOLUNTEER -> volunteerDao.findAllByShiftPlanAndVolunteerIds(
                    ConvertUtil.idToLong(filter.getRelatedShiftPlanId()),
                    filter.getRelatedVolunteerIds()
                );
                case PLANNER -> volunteerDao.findAllByShiftPlanAndPlannerIds(
                    ConvertUtil.idToLong(filter.getRelatedShiftPlanId()),
                    filter.getRelatedVolunteerIds()
                );
                case ADMIN -> throw new NotImplementedException(); // should be handled at root
            }).stream().map(Volunteer::getId).collect(Collectors.toSet());
        } else {
            return (switch (filter.getReceiverAccessLevel()) {
                case VOLUNTEER -> volunteerDao.findAllByShiftPlan(
                    ConvertUtil.idToLong(filter.getRelatedShiftPlanId())
                );
                case PLANNER -> volunteerDao.findAllPlannersByShiftPlan(
                    ConvertUtil.idToLong(filter.getRelatedShiftPlanId())
                );
                case ADMIN -> throw new NotImplementedException(); // should be handled at root
            }).stream().map(Volunteer::getId).collect(Collectors.toSet());
        }
    }

    private Collection<String> filterByVolunteerIds(RecipientsFilterDto filter, Collection<String> recipientIds) {
        if (filter.getRelatedVolunteerIds() != null) {
            return (switch (filter.getReceiverAccessLevel()) {
                case VOLUNTEER -> volunteerDao.findAllByVolunteerIds(
                    filter.getRelatedVolunteerIds()
                );
                case PLANNER -> volunteerDao.findAllByPlannerIds(
                    filter.getRelatedVolunteerIds()
                );
                case ADMIN -> throw new NotImplementedException(); // should be handled at root
            }).stream().map(Volunteer::getId).collect(Collectors.toSet());
        } else {
            return recipientIds;
        }
    }

    private Collection<String> filterAdminsById(RecipientsFilterDto filter) {
        var admins = userDirectoryService.getAllAdmins();
        if (filter.getRelatedVolunteerIds() != null) {
            return admins.stream()
                .map(UserInfo::id)
                .filter(id -> filter.getRelatedVolunteerIds().contains(id))
                .collect(Collectors.toSet());
        } else {
            return admins.stream()
                .map(UserInfo::id)
                .collect(Collectors.toSet());
        }
    }

    private void validateFilter(RecipientsFilterDto filter) {
        if (filter.getRelatedEventId() != null && filter.getRelatedShiftPlanId() != null) {
            throw new BadRequestException("Recipients can be filtered either by associated event or shift plan.");
        }
    }
}
