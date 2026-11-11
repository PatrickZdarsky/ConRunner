package at.control.shiftcontrol.shift.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class PositionConstraintId implements Serializable {
    private long sourcePositionSlotId;
    private long targetPositionSlotId;

    public static PositionConstraintId of(long sourcePositionSlotId, long targetPositionSlotId) {
        return PositionConstraintId.builder()
                .sourcePositionSlotId(sourcePositionSlotId)
                .targetPositionSlotId(targetPositionSlotId)
                .build();
    }
}
