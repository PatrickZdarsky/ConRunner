package at.conrunner.shift.dto.plannerdashboard;

import java.util.Collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.conrunner.shift.lib.type.AssignmentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentFilterDto {
    @NotNull
    private Collection<AssignmentStatus> statuses;
}
