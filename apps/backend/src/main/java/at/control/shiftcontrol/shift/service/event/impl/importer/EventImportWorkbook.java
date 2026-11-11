package at.control.shiftcontrol.shift.service.event.impl.importer;

import java.time.Instant;
import java.util.List;

public record EventImportWorkbook(
    EventRow eventRow,
    List<LocationRow> locations,
    List<ActivityRow> activities,
    List<ShiftRow> shifts
) {
    public record EventRow(
        String eventName,
        Instant start,
        Instant end,
        String shortDescription,
        String longDescription
    ) {
    }

    public record LocationRow(
        String locationName,
        String description,
        String url,
        int excelRow
    ) {
    }

    public record ActivityRow(
        String activityName,
        String description,
        Instant start,
        Instant end,
        String locationName,
        int excelRow
    ) {
    }

    public record ShiftRow(
        String shiftPlanName,
        String shiftName,
        Instant start,
        Instant end,
        String locationName,
        String activityName,
        String shortDescription,
        String longDescription,
        int excelRow
    ) {
    }
}
