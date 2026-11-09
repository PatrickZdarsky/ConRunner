package at.control.shiftcontrol.shift.dto.event.schedule;

import java.util.Collection;

import at.control.shiftcontrol.shift.dto.activity.ActivityDto;
import at.control.shiftcontrol.shift.dto.location.LocationDto;
import at.control.shiftcontrol.shift.dto.shift.ShiftColumnDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleContentDto {
    @NotNull
    @Valid
    private LocationDto location;

    @NotNull
    @Valid
    private Collection<ActivityDto> activities;

    @NotNull
    @Valid
    private Collection<ShiftColumnDto> shiftColumns;
}
