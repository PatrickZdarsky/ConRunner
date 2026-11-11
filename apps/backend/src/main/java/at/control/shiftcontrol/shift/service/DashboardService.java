package at.control.shiftcontrol.shift.service;

import java.util.UUID;
import at.control.shiftcontrol.shift.dto.event.EventsDashboardOverviewDto;

import lombok.NonNull;

public interface DashboardService {
    @NonNull EventsDashboardOverviewDto getDashboardOverviewsOfAllShiftPlans(@NonNull String eventId, @NonNull UUID userId);
}
