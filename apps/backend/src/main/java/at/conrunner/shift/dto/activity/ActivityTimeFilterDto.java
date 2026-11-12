package at.conrunner.shift.dto.activity;

import java.time.Instant;

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
public class ActivityTimeFilterDto {
    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    @NotNull
    @Min(0)
    private int toleranceInMinutes;
}
