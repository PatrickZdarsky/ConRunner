package at.control.shiftcontrol.shift.dto.event;

import java.util.Collection;

import at.control.shiftcontrol.shift.dto.OverallStatisticsDto;
import at.control.shiftcontrol.shift.dto.OwnStatisticsDto;
import at.control.shiftcontrol.shift.dto.role.RoleDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanDto;
import jakarta.validation.Valid;
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
public class EventShiftPlansOverviewDto {
    @NotNull
    @Valid
    private EventDto eventOverview;

    @NotNull
    @Valid
    private OwnStatisticsDto ownEventStatistics;

    @NotNull
    @Valid
    private OverallStatisticsDto overallEventStatistics;

    @NotNull
    @Min(0)
    private int rewardPoints;

    @NotNull
    @Valid
    private Collection<ShiftPlanDto> shiftPlans;

    @NotNull
    @Valid
    private Collection<RoleDto> roles;
}
