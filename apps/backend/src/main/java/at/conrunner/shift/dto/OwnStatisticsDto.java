package at.conrunner.shift.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnStatisticsDto {
    @NotNull
    private double totalHours;

    @NotNull
    @Min(0)
    private int totalShifts;

    @NotNull
    @Min(0)
    private int busyDays;
}
