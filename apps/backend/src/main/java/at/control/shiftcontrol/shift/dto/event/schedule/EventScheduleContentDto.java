package at.control.shiftcontrol.shift.dto.event.schedule;

import java.time.LocalDate;
import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventScheduleContentDto {
    @NotNull
    private LocalDate date;

    @NotNull
    @Valid
    private Collection<ScheduleContentDto> scheduleContentDtos;

    @NotNull
    @Valid
    private ScheduleContentNoLocationDto scheduleContentNoLocationDto;
}
