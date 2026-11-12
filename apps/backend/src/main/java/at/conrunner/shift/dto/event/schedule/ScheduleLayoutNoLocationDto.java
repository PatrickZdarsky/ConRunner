package at.conrunner.shift.dto.event.schedule;

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
public class ScheduleLayoutNoLocationDto {
    @NotNull
    @Min(0)
    private int requiredShiftColumns;
}
