package at.conrunner.shift.dto.event.schedule;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventScheduleLayoutDto {
    @NotNull
    @Valid
    private Collection<ScheduleLayoutDto> scheduleLayoutDtos;

    @NotNull
    @Valid
    private ScheduleLayoutNoLocationDto scheduleLayoutNoLocationDto;

    @NotNull
    @Valid
    private ScheduleStatisticsDto scheduleStatistics;
}
