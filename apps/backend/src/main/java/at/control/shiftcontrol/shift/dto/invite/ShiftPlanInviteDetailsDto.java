package at.control.shiftcontrol.shift.dto.invite;

import at.shiftcontrol.shiftservice.dto.event.EventDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPlanInviteDetailsDto {
    @NotNull
    @Min(0)
    private int attendingVolunteerCount;

    @NotNull
    private boolean joined;

    @NotNull
    private boolean upgradeToPlannerPossible;

    @NotNull
    private boolean extensionOfRolesPossible;

    @NotNull
    @Valid
    private ShiftPlanInviteDto inviteDto;

    @NotNull
    @Valid
    private EventDto eventDto;
}
