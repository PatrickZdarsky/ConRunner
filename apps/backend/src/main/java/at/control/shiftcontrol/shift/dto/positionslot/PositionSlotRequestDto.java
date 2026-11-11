package at.control.shiftcontrol.shift.dto.positionslot;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionSlotRequestDto {
    @NotNull
    private String acceptedRewardPointsConfigHash;
}
