package at.conrunner.shift.dto.event.schedule;

import java.util.Collection;

import at.conrunner.shift.dto.activity.ActivityDto;
import at.conrunner.shift.dto.event.EventDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ActivityScheduleDto {
    @Valid
    @NotNull
    private EventDto event;

    @NotNull
    @Valid
    private Collection<ActivityDto> activities;

}
