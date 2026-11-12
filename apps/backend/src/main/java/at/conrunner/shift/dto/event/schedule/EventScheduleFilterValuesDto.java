package at.conrunner.shift.dto.event.schedule;

import java.time.Instant;
import java.util.Collection;

import at.conrunner.shift.dto.location.LocationDto;
import at.conrunner.shift.dto.role.RoleDto;
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
