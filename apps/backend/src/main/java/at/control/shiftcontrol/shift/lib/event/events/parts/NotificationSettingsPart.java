package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.util.Set;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import at.control.shiftcontrol.shift.entity.NotificationSettings;
import at.control.shiftcontrol.shift.lib.type.NotificationChannel;
import at.control.shiftcontrol.shift.lib.type.NotificationType;

@AllArgsConstructor
@Data
public class NotificationSettingsPart {
    @NotNull
    private NotificationType type;
    @NotNull
    private Set<NotificationChannel> channels;

    public static NotificationSettingsPart of(NotificationSettings dto) {
        return new NotificationSettingsPart(
            dto.getType(),
            dto.getChannels()
        );
    }
}
