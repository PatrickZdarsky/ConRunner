package at.control.shiftcontrol.shift.dto.rewardpoints;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardPointsTransactionDto {
    @NotNull
    String userId;
    @NotNull
    long eventId;
    Long shiftPlanId;
    Long positionSlotId;
    @NotNull
    int pointsSnapshot;
    @NotNull
    String sourceKey;
    Map<String, Object> metadata;
}
