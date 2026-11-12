package at.conrunner.shift.dto.rewardpoints;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardPointsShareTokenCreateRequestDto {
    @NotNull
    private String name;
}
