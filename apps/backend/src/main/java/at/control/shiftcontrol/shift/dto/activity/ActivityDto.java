package at.control.shiftcontrol.shift.dto.activity;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.dto.location.LocationDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivityDto {
    @NotNull
    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    @Valid
    private LocationDto location;

    @NotNull
    private boolean readOnly;
}
