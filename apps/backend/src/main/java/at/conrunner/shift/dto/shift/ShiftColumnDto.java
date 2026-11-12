package at.conrunner.shift.dto.shift;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftColumnDto {
    @NotNull
    @Min(0)
    private int columnIndex;

    @NotNull
    @Valid
    private ShiftDto shiftDto;
}
