package at.control.shiftcontrol.shift.dto.event;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import at.shiftcontrol.shiftservice.dto.OwnStatisticsDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanDashboardOverviewDto;

@Data
@Builder
public class EventsDashboardOverviewDto {
    @NotNull
    @Valid
    private Collection<ShiftPlanDashboardOverviewDto> shiftPlanDashboardOverviewDtos;

    @NotNull
    @Valid
    private OwnStatisticsDto ownStatisticsDto;
}
