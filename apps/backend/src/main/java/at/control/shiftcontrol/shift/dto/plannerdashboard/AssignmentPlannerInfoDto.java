package at.control.shiftcontrol.shift.dto.plannerdashboard;

import java.util.Collection;

import at.shiftcontrol.shiftservice.dto.assignment.AssignmentContextDto;

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
public class AssignmentPlannerInfoDto {
    @NotNull
    private long shiftId;

    @NotNull
    @Valid
    private Collection<AssignmentContextDto> slots;
}
