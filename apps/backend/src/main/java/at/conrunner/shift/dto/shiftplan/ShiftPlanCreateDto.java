package at.conrunner.shift.dto.shiftplan;

import at.conrunner.shift.dto.invite.ShiftPlanInviteDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPlanCreateDto {
    @NotNull
    @Valid
    private ShiftPlanDto shiftPlan;

    @NotNull
    @Valid
    private ShiftPlanInviteDto volunteerInvite;

    @NotNull
    @Valid
    private ShiftPlanInviteDto plannerInvite;
}
