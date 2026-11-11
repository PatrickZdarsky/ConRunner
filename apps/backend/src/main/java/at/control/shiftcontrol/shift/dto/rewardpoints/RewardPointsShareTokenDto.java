package at.control.shiftcontrol.shift.dto.rewardpoints;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardPointsShareTokenDto {
    @NotNull
    private String id;

    @NotNull
    private String token;

    @NotNull
    private String name;

    @NotNull
    private Instant createdAt;
}
