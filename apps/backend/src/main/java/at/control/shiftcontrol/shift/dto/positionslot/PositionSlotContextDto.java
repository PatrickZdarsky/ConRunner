package at.control.shiftcontrol.shift.dto.positionslot;

import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsDto;
import at.control.shiftcontrol.shift.dto.role.RoleDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionSlotContextDto {
    @NotNull
    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull
    private boolean skipAutoAssignment;

    @NotNull
    @Min(0)
    private int desiredVolunteerCount;

    @Valid
    private RoleDto role;

    @NotNull
    @Valid
    private RewardPointsDto rewardPointsDto;
}
