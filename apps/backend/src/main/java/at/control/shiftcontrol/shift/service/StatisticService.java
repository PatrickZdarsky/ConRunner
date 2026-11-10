package at.control.shiftcontrol.shift.service;

import java.util.UUID;
import java.util.List;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.dto.OverallStatisticsDto;
import at.control.shiftcontrol.shift.dto.OwnStatisticsDto;
import at.control.shiftcontrol.shift.dto.event.schedule.ScheduleStatisticsDto;

import lombok.NonNull;

public interface StatisticService {
    @NonNull OwnStatisticsDto getOwnStatisticsOfShifts(@NonNull List<Shift> userShifts);

    @NonNull OverallStatisticsDto getOverallShiftPlanStatistics(@NonNull ShiftPlan shiftPlan);

    @NonNull OverallStatisticsDto getOverallEventStatistics(@NonNull Event event);

    @NonNull OwnStatisticsDto getOwnStatisticsOfShiftPlans(@NonNull List<ShiftPlan> shiftPlans, @NonNull UUID userId);

    @NonNull ScheduleStatisticsDto getShiftPlanScheduleStatistics(@NonNull List<Shift> shifts, @NonNull Volunteer volunteer);
}
