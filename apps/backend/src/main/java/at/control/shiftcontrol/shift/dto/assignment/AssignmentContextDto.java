package at.control.shiftcontrol.shift.dto.assignment;

import at.shiftcontrol.shiftservice.dto.positionslot.PositionSlotContextDto;
import at.shiftcontrol.shiftservice.dto.shift.ShiftContextDto;
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
public class AssignmentContextDto {
    @Valid
    @NotNull
    AssignmentDto assignment;

    @Valid
    @NotNull
    ShiftContextDto shiftContext;

    @Valid
    @NotNull
    PositionSlotContextDto positionSlotContext;
}
