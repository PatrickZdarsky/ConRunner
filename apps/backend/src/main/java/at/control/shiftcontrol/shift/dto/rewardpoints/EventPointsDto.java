package at.control.shiftcontrol.shift.dto.rewardpoints;

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
public class EventPointsDto {
    @NotNull
    private String eventId;

    @NotNull
    @Min(0)
    private int points;
}
