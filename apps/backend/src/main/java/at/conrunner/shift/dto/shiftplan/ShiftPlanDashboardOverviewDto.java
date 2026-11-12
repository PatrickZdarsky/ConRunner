package at.conrunner.shift.dto.shiftplan;

import java.util.Collection;

import at.conrunner.shift.dto.assignment.AssignmentContextDto;
import at.conrunner.shift.dto.trade.TradeInfoDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import at.conrunner.shift.dto.OverallStatisticsDto;
import at.conrunner.shift.dto.OwnStatisticsDto;
import at.conrunner.shift.dto.event.EventDto;
import at.conrunner.shift.dto.shift.ShiftDto;

@Data
@Builder
public class ShiftPlanDashboardOverviewDto {
    @NotNull
    @Valid
    private ShiftPlanDto shiftPlan;

    @NotNull
    @Valid
    private EventDto eventOverview;

    @NotNull
    @Valid
    private OwnStatisticsDto ownShiftPlanStatistics;

    @NotNull
    @Valid
    private OverallStatisticsDto overallShiftPlanStatistics;

    @NotNull
    @Min(0)
    private int rewardPoints;

    @NotNull
    @Valid
    private Collection<ShiftDto> shifts;

    @NotNull
    @Valid
    private Collection<TradeInfoDto> trades;

    @NotNull
    @Valid
    private Collection<AssignmentContextDto> auctions;
}
