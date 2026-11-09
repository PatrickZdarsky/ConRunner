package at.control.shiftcontrol.shift.dto.userprofile;

import java.util.Set;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.shiftcontrol.lib.type.NotificationChannel;
import at.shiftcontrol.lib.type.NotificationType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsDto {
    @NotNull
    private NotificationType type;
    @NotNull
    private Set<NotificationChannel> channels;
}
