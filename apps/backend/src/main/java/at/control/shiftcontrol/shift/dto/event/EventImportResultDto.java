package at.control.shiftcontrol.shift.dto.event;

import java.util.Collection;

import at.shiftcontrol.shiftservice.dto.shift.ShiftDto;
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
public class EventImportResultDto {
    @NotNull
    @Valid
    private EventDto event;

    @NotNull
    @Valid
    private Collection<ShiftDto> shifts;
}
