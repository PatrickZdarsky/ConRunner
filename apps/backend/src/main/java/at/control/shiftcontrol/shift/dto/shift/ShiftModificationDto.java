package at.control.shiftcontrol.shift.dto.shift;

import java.time.Instant;

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
public class ShiftModificationDto {
    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String shortDescription;

    @Size(max = 1024)
    private String longDescription;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    private String activityId;

    private String locationId;

    @NotNull
    @Min(0)
    private int bonusRewardPoints;
}
