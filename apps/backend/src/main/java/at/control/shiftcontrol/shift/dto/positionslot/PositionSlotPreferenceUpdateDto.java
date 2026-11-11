package at.control.shiftcontrol.shift.dto.positionslot;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PositionSlotPreferenceUpdateDto {
    @NotNull
    @Min(-10)
    @Max(10)
    private int preferenceValue;
}
