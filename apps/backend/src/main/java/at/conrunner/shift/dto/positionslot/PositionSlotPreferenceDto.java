package at.conrunner.shift.dto.positionslot;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PositionSlotPreferenceDto {
    @NotNull
    @Min(0)
    private int preferenceValue;
}
