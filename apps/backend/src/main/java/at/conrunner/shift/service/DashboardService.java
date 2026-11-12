package at.conrunner.shift.service;

import java.util.UUID;
import at.conrunner.shift.dto.event.EventsDashboardOverviewDto;

import lombok.NonNull;

public interface DashboardService {
    @NonNull EventsDashboardOverviewDto getDashboardOverviewsOfAllShiftPlans(@NonNull String eventId, @NonNull UUID userId);
}
