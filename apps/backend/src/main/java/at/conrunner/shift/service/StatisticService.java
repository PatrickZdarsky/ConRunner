package at.conrunner.shift.service;

import java.util.UUID;
import java.util.Collection;
import java.util.List;

import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.dto.OverallStatisticsDto;
import at.conrunner.shift.dto.OwnStatisticsDto;
import at.conrunner.shift.dto.event.schedule.ScheduleStatisticsDto;

import lombok.NonNull;

public interface StatisticService {
    @NonNull OwnStatisticsDto getOwnStatisticsOfShifts(@NonNull List<Shift> userShifts);

    @NonNull OverallStatisticsDto getOverallShiftPlanStatistics(@NonNull ShiftPlan shiftPlan);

    @NonNull OverallStatisticsDto getOverallEventStatistics(@NonNull Collection<ShiftPlan> eventPlans);

    @NonNull OwnStatisticsDto getOwnStatisticsOfShiftPlans(@NonNull List<ShiftPlan> shiftPlans, @NonNull UUID userId);

    @NonNull ScheduleStatisticsDto getShiftPlanScheduleStatistics(@NonNull List<Shift> shifts, @NonNull Volunteer volunteer);
}
