package at.conrunner.shift.dto.rewardpoints;

import java.util.Collection;

import at.conrunner.shift.dto.event.EventDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardPointsExportDto {
    @NotNull
    @Valid
    private EventDto event;

    @NotNull
    private boolean eventFinished;

    @NotNull
    @Valid
    private Collection<VolunteerPointsDto> volunteerPoints;

}
