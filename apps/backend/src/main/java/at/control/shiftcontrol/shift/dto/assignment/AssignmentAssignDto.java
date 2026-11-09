package at.control.shiftcontrol.shift.dto.assignment;

import java.util.Collection;

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
public class AssignmentAssignDto {
    @NotNull
    private String positionSlotId;

    @NotNull
    @Valid
    private Collection<String> volunteerIds;
}
