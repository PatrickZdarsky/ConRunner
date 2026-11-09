package at.control.shiftcontrol.shift.dto.shiftplan;

import java.util.Collection;

import at.control.shiftcontrol.shift.dto.assignment.AssignmentContextDto;
import at.control.shiftcontrol.shift.dto.trade.TradeInfoDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.OverallStatisticsDto;
import at.control.shiftcontrol.shift.dto.OwnStatisticsDto;
import at.control.shiftcontrol.shift.dto.event.EventDto;
import at.control.shiftcontrol.shift.dto.shift.ShiftDto;
import at.control.shiftcontrol.shift.dto.trade.TradeDto;

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
