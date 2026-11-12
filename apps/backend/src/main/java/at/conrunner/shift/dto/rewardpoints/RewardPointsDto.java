package at.conrunner.shift.dto.rewardpoints;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardPointsDto {
    @NotNull
    @Min(0)
    private int currentRewardPoints;

    @Min(0)
    private Integer overrideRewardPoints;

    @NotNull
    private String rewardPointsConfigHash;
}
