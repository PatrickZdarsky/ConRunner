package at.conrunner.shift.dto.shiftplan;

import java.util.ArrayList;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPlanStatisticsDto {
    @Schema(description = "Total amount of shifts in the shift plan.")
    private int shiftCount;
    @Schema(description = "Total amount of volunteers in the shift plan.")
    private int volunteerCount;
    @Schema(description = "Total amount of positions in the shift plan.")
    private int totalPositions;
    @Schema(description = "Amount of positions in the shift plan that are currently assigned to a volunteer.")
    private int currentPositionAssignedCount;
    @Schema(description = "Total hours of all shifts in the shift plan, including unfilled positions.")
    private double totalShiftHours;
    @Schema(description = "Total hours of all positions in the shift plan times amount per position, including unfilled positions.")
    private double totalPositionVolunteerHours;
    @Schema(description = "Currently assigned hours of all positions in the shift plan times amount per position, excluding unfilled positions.")
    private double currentPositionVolunteerHours;
    @Schema(description = "Statistics for each volunteer in the shift plan.")
    private List<VolunteerShiftStatisticsDto> volunteerStats;
    @Schema(description = "Open position slots")
    private List<PositionInfoDto> openPositionStats = new ArrayList<>();


    @Data
    @RequiredArgsConstructor
    public static class PositionInfoDto {
        private final long positionId;
        private final String positionName;
        private final long shiftId;
        private final String shiftName;
        private final int positionCount;
        private final double durationHours;
    }

    @Data
    @RequiredArgsConstructor
    public static class VolunteerShiftStatisticsDto {
        private final String id;
        private final String username;
        private int positionCount;
        private double totalHours;

        private List<AssignmentInfoDto> assignments = new ArrayList<>();
    }

    @Data
    @RequiredArgsConstructor
    public static class AssignmentInfoDto {
        private final long positionId;
        private final String positionName;
        private final long shiftId;
        private final String shiftName;
        private final double durationHours;
    }
}
