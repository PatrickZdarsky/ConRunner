package at.control.shiftcontrol.shift.dto.rewardpoints;

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
public class VolunteerPointsDto {
    @NotNull
    private String volunteerId;

    @NotNull
    private String name;

    @NotNull
    private String email;

    @NotNull
    @Min(0)
    private int rewardPoints;
}
