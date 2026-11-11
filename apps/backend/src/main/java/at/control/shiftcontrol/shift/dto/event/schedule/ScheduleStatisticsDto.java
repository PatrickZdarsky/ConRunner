package at.control.shiftcontrol.shift.dto.event.schedule;

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
public class ScheduleStatisticsDto {
    @NotNull
    @Min(0)
    private int totalShifts;

    @NotNull
    private double totalHours;

    @NotNull
    @Min(0)
    private int unassignedCount;

    @NotNull
    @Min(0)
    private int shiftCountForSignup;
}
