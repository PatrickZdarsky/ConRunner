package at.control.shiftcontrol.shift.service.userprofile.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.NotificationSettings;
import at.control.shiftcontrol.shift.lib.event.events.NotificationSettingsEvent;
import at.control.shiftcontrol.shift.lib.exception.IllegalStateException;
import at.control.shiftcontrol.shift.lib.type.NotificationChannel;
import at.control.shiftcontrol.shift.lib.type.NotificationType;
import at.control.shiftcontrol.shift.dao.NotificationSettingsDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.userprofile.NotificationSettingsDto;
import at.control.shiftcontrol.shift.mapper.NotificationSettingsMapper;
import at.control.shiftcontrol.shift.service.VolunteerService;
import at.control.shiftcontrol.shift.service.userprofile.NotificationService;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationSettingsDao notificationSettingsDao;
    private final VolunteerDao volunteerDao;
    private final VolunteerService volunteerService;
    private final ApplicationEventPublisher publisher;

    @Override
    public Collection<NotificationSettingsDto> getNotificationsForUser(String userId) {
        return NotificationSettingsMapper.toDto(getSettingsOrDefault(userId, false));
    }

    @Override
    public NotificationSettingsDto updateNotificationSetting(@NonNull String userId, @NonNull NotificationSettingsDto settingsDto) {
        Collection<NotificationSettings> existingOrDefault = getSettingsOrDefault(userId, true);
        // find setting for type
        var toUpdate = existingOrDefault.stream()
            .filter(ns -> ns.getType() == settingsDto.getType())
            .findFirst().orElseThrow(() -> new IllegalStateException("Could not find notification setting for type " + settingsDto.getType()));

        // update setting
        toUpdate.setChannels(
            settingsDto.getChannels().isEmpty()
                ? EnumSet.noneOf(NotificationChannel.class) // NONE
                : EnumSet.copyOf(settingsDto.getChannels())
        );
        toUpdate = notificationSettingsDao.save(toUpdate);

        publisher.publishEvent(NotificationSettingsEvent.settingsUpdated(userId, toUpdate));
        return NotificationSettingsMapper.toDto(toUpdate);
    }

    private Collection<NotificationSettings> getSettingsOrDefault(String userId, boolean createVolunteerIfMissing) {
        var user = volunteerDao.findById(userId)
            .orElseGet(() -> createVolunteerIfMissing ? volunteerService.getOrCreate(userId) : null);
        Collection<NotificationSettings> existingSettings = notificationSettingsDao.findAllByUserId(userId);

        // create map for lookup
        Map<NotificationType, NotificationSettings> settingsByType = existingSettings.stream()
            .collect(Collectors.toMap(NotificationSettings::getType, ns -> ns));

        // return saved settings if present, else default ones
        return Arrays.stream(NotificationType.values())
            .map(type -> {
                NotificationSettings existingSetting = settingsByType.get(type);
                if (existingSetting != null) {
                    return existingSetting;
                } else {
                    return NotificationSettings.builder()
                        .type(type)
                        .channels(EnumSet.of(NotificationChannel.PUSH)) // default channels
                        .user(user)
                        .build();
                }
            }).toList();
    }
}
