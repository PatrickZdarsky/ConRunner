package at.control.shiftcontrol.shift.lib.event.events.parts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import at.control.shiftcontrol.shift.entity.PositionSlot;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionSlotPart {
    private long positionSlotId;
    private String positionSlotName;
    private String positionSlotDescription;
    private ShiftPlanRefPart shiftPlanRefPart;

    @NonNull
    public static PositionSlotPart of(@NonNull PositionSlot positionSlot) {
        return PositionSlotPart.builder()
            .positionSlotId(positionSlot.getId())
            .positionSlotName(positionSlot.getName())
            .positionSlotDescription(positionSlot.getDescription())
            .shiftPlanRefPart(ShiftPlanRefPart.of(positionSlot.getShift().getShiftPlan()))
            .build();
    }
}
