package at.control.shiftcontrol.shift.dto.positionslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.PositionSignupState;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionSlotJoinErrorDto {
    private PositionSignupState state;
    private String message;

    public PositionSlotJoinErrorDto(PositionSignupState state) {
        this.state = state;
        this.message = state.getMessage();
    }
}
