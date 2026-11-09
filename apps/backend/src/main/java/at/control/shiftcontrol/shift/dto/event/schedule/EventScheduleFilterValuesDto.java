package at.control.shiftcontrol.shift.dto.event.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

import at.control.shiftcontrol.shift.dto.location.LocationDto;
import at.control.shiftcontrol.shift.dto.role.RoleDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventScheduleFilterValuesDto {
    @NotNull
    @Valid
    private Collection<LocationDto> locations;

    @NotNull
    @Valid
    private Collection<RoleDto> roles;

    private Instant firstDate;

    private Instant lastDate;
}
