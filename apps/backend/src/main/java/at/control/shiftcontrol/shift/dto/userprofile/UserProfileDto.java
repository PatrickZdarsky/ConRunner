package at.control.shiftcontrol.shift.dto.userprofile;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.shiftcontrol.shiftservice.dto.role.RoleDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    @NotNull
    @Valid
    private AccountInfoDto account;

    @NotNull
    @Valid
    private Collection<NotificationSettingsDto> notifications;

    @NotNull
    @Valid
    private Collection<RoleDto> assignedRoles;

    @NotNull
    private Collection<String> planningPlans;

    @NotNull
    private Collection<String> volunteeringPlans;

    @NotNull
    private Collection<String> planningEvents;

    @NotNull
    private Collection<String> volunteeringEvents;

    private String calendarShareId;
}
