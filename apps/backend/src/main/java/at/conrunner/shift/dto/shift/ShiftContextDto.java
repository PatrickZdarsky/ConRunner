package at.conrunner.shift.dto.shift;

import java.time.Instant;

import at.conrunner.shift.dto.location.LocationDto;
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
public class ShiftContextDto {
    @NotNull
    private String id;

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

    @Valid
    private LocationDto location;

    @NotNull
    @Min(0)
    private int bonusRewardPoints;
}
