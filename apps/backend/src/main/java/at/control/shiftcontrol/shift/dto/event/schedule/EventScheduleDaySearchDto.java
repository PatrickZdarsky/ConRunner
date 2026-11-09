package at.control.shiftcontrol.shift.dto.event.schedule;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventScheduleDaySearchDto extends EventScheduleFilterDto {
    @NotNull
    private LocalDate date;
}
