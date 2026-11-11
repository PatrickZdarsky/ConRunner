package at.control.shiftcontrol.shift.dto.leaderboard;

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
public class RankDto {
    @NotNull
    private int rank;

    @NotNull
    @Min(0)
    private long hours;

    @NotNull
    private String name;
}
