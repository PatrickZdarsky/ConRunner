package at.conrunner.shift.dto.positionslot;

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
public class PositionSlotModificationDto {
    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull
    private boolean skipAutoAssignment;

    private String roleId;

    @NotNull
    @Min(0)
    private int desiredVolunteerCount;

    @Min(0)
    private Integer overrideRewardPoints;
}
