package at.control.shiftcontrol.shift.dto.shiftplan;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import at.control.shiftcontrol.shift.lib.type.LockStatus;

@Data
@Builder
public class ShiftPlanPatchStatusDto {
    @NotNull
    private LockStatus lockStatus;
}
