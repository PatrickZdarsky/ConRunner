package at.control.shiftcontrol.shift.dto.invite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShiftPlanJoinRequestDto {
    @NotBlank
    private String inviteCode;
}
