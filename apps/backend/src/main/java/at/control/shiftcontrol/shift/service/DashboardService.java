package at.control.shiftcontrol.shift.service;

import at.shiftcontrol.shiftservice.dto.event.EventsDashboardOverviewDto;

import lombok.NonNull;

public interface DashboardService {
    @NonNull EventsDashboardOverviewDto getDashboardOverviewsOfAllShiftPlans(@NonNull String eventId, @NonNull String userId);
}
