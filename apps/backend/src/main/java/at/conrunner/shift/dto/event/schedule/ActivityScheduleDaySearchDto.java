package at.conrunner.shift.dto.event.schedule;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityScheduleDaySearchDto {
    @NotNull
    private LocalDate date;
}
