package at.conrunner.shift.dto.assignment;

import at.conrunner.shift.dto.positionslot.PositionSlotContextDto;
import at.conrunner.shift.dto.shift.ShiftContextDto;
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
