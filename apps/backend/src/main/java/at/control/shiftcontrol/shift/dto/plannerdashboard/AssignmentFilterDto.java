package at.control.shiftcontrol.shift.dto.plannerdashboard;

import java.util.Collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentFilterDto {
    @NotNull
    private Collection<AssignmentStatus> statuses;
}
