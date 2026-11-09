package at.control.shiftcontrol.shift.service;

import java.util.List;

import at.shiftcontrol.lib.entity.Event;
import at.shiftcontrol.lib.entity.Shift;
import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.shiftservice.dto.OverallStatisticsDto;
import at.shiftcontrol.shiftservice.dto.OwnStatisticsDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.ScheduleStatisticsDto;

import lombok.NonNull;

public interface StatisticService {
    @NonNull OwnStatisticsDto getOwnStatisticsOfShifts(@NonNull List<Shift> userShifts);

    @NonNull OverallStatisticsDto getOverallShiftPlanStatistics(@NonNull ShiftPlan shiftPlan);

    @NonNull OverallStatisticsDto getOverallEventStatistics(@NonNull Event event);

    @NonNull OwnStatisticsDto getOwnStatisticsOfShiftPlans(@NonNull List<ShiftPlan> shiftPlans, @NonNull String userId);

    @NonNull ScheduleStatisticsDto getShiftPlanScheduleStatistics(@NonNull List<Shift> shifts, @NonNull Volunteer volunteer);
}
