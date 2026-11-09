package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;

import lombok.NoArgsConstructor;

import at.shiftcontrol.lib.entity.NotificationSettings;
import at.shiftcontrol.shiftservice.dto.userprofile.NotificationSettingsDto;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class NotificationSettingsMapper {
    public static NotificationSettings toEntity(NotificationSettingsDto dto) {
        return NotificationSettings.builder()
            .channels(dto.getChannels())
            .type(dto.getType())
            .build();
    }

    public static NotificationSettingsDto toDto(NotificationSettings dto) {
        return NotificationSettingsDto.builder()
            .channels(dto.getChannels())
            .type(dto.getType())
            .build();
    }

    public static Collection<NotificationSettingsDto> toDto(Collection<NotificationSettings> dtos) {
        return dtos.stream().map(NotificationSettingsMapper::toDto).toList();
    }
}
