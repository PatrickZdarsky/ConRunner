package at.control.shiftcontrol.shift.service.event.impl.importer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import at.shiftcontrol.lib.exception.ValidationException;

@Component
public class EventImportWorkbookParser {
    public static final String VALIDATION_CONTEXT = "Import Event (XLSX)";
    static final String SHEET_EVENT = "Event";
    static final String SHEET_LOCATIONS = "Locations";
    static final String SHEET_ACTIVITIES = "Activities";
    static final String SHEET_SHIFTS = "Shifts";

    public @NonNull EventImportWorkbook parse(@NonNull Workbook workbook) {
        ValidationException.Builder validation = ValidationException.builder()
            .context(VALIDATION_CONTEXT);

        Sheet eventSheet = workbook.getSheet(SHEET_EVENT);
        if (eventSheet == null) {
            validation.error("file", "Missing sheet '" + SHEET_EVENT + "'.");
        }

        Sheet locationsSheet = workbook.getSheet(SHEET_LOCATIONS);
        if (locationsSheet == null) {
            validation.error("file", "Missing sheet '" + SHEET_LOCATIONS + "'.");
        }

        Sheet activitiesSheet = workbook.getSheet(SHEET_ACTIVITIES);
        if (activitiesSheet == null) {
            validation.error("file", "Missing sheet '" + SHEET_ACTIVITIES + "'.");
        }

        Sheet shiftsSheet = workbook.getSheet(SHEET_SHIFTS);
        if (shiftsSheet == null) {
            validation.error("file", "Missing sheet '" + SHEET_SHIFTS + "'.");
        }

        validation.throwIfInvalid();

        EventImportWorkbook workbookModel = new EventImportWorkbook(
            parseEventSheet(eventSheet, validation),
            parseLocationsSheet(locationsSheet, validation),
            parseActivitiesSheet(activitiesSheet, validation),
            parseShiftsSheet(shiftsSheet, validation)
        );

        validateWorkbook(workbookModel, validation);
        validation.throwIfInvalid();
        return workbookModel;
    }

    private EventImportWorkbook.EventRow parseEventSheet(Sheet sheet, ValidationException.Builder validation) {
        Map<String, Integer> header = readHeader(sheet, 0);

        String eventName = readStringCell(sheet, 2, header, "event_name", validation, true);
        Instant start = readInstantCell(sheet, 2, header, "start_time_utc", validation, true);
        Instant end = readInstantCell(sheet, 2, header, "end_time_utc", validation, true);
        String shortDescription = readStringCell(sheet, 2, header, "short_description", validation, false);
        String longDescription = readStringCell(sheet, 2, header, "long_description", validation, false);

        return new EventImportWorkbook.EventRow(normalizeName(eventName), start, end, shortDescription, longDescription);
    }

    private List<EventImportWorkbook.LocationRow> parseLocationsSheet(Sheet sheet, ValidationException.Builder validation) {
        Map<String, Integer> header = readHeader(sheet, 0);
        List<EventImportWorkbook.LocationRow> locations = new ArrayList<>();

        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            if (isRowEmpty(sheet.getRow(rowIndex))) {
                continue;
            }

            String name = readStringCell(sheet, rowIndex, header, "location_name", validation, true);
            String description = readStringCell(sheet, rowIndex, header, "description", validation, false);
            String url = readStringCell(sheet, rowIndex, header, "url", validation, false);

            locations.add(new EventImportWorkbook.LocationRow(
                normalizeName(name),
                description,
                url,
                rowIndex + 1
            ));
        }

        return locations;
    }

    private List<EventImportWorkbook.ActivityRow> parseActivitiesSheet(Sheet sheet, ValidationException.Builder validation) {
        Map<String, Integer> header = readHeader(sheet, 0);
        List<EventImportWorkbook.ActivityRow> activities = new ArrayList<>();

        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            if (isRowEmpty(sheet.getRow(rowIndex))) {
                continue;
            }

            String name = readStringCell(sheet, rowIndex, header, "activity_name", validation, true);
            String description = readStringCell(sheet, rowIndex, header, "description", validation, false);
            Instant start = readInstantCell(sheet, rowIndex, header, "start_time_utc", validation, true);
            Instant end = readInstantCell(sheet, rowIndex, header, "end_time_utc", validation, true);
            String locationName = readStringCell(sheet, rowIndex, header, "location_name", validation, false);

            activities.add(new EventImportWorkbook.ActivityRow(
                normalizeName(name),
                description,
                start,
                end,
                normalizeName(locationName),
                rowIndex + 1
            ));
        }

        return activities;
    }

    private List<EventImportWorkbook.ShiftRow> parseShiftsSheet(Sheet sheet, ValidationException.Builder validation) {
        Map<String, Integer> header = readHeader(sheet, 0);
        List<EventImportWorkbook.ShiftRow> shifts = new ArrayList<>();

        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            if (isRowEmpty(sheet.getRow(rowIndex))) {
                continue;
            }

            String shiftPlanName = readStringCell(sheet, rowIndex, header, "shift_plan_name", validation, true);
            String shiftName = readStringCell(sheet, rowIndex, header, "shift_name", validation, true);
            Instant start = readInstantCell(sheet, rowIndex, header, "start_time_utc", validation, true);
            Instant end = readInstantCell(sheet, rowIndex, header, "end_time_utc", validation, true);
            String locationName = readStringCell(sheet, rowIndex, header, "location_name", validation, false);
            String activityName = readStringCell(sheet, rowIndex, header, "activity_name", validation, false);
            String shortDescription = readStringCell(sheet, rowIndex, header, "short_description", validation, false);
            String longDescription = readStringCell(sheet, rowIndex, header, "long_description", validation, false);

            shifts.add(new EventImportWorkbook.ShiftRow(
                normalizeName(shiftPlanName),
                normalizeName(shiftName),
                start,
                end,
                normalizeName(locationName),
                normalizeName(activityName),
                shortDescription,
                longDescription,
                rowIndex + 1
            ));
        }

        if (shifts.isEmpty()) {
            validation.error("file", "Sheet '" + sheet.getSheetName() + "' contains no shift entries.");
        }

        return shifts;
    }

    private void validateWorkbook(EventImportWorkbook workbook, ValidationException.Builder validation) {
        EventImportWorkbook.EventRow eventRow = workbook.eventRow();

        if (eventRow.start() != null && eventRow.end() != null && !eventRow.start().isBefore(eventRow.end())) {
            validation.error("Event.start_time_utc", "Event start_time_utc must be before end_time_utc.");
        }

        validateUniqueNames(
            workbook.locations().stream().collect(Collectors.groupingBy(location -> normalizeKey(location.locationName()))),
            "Locations.location_name",
            "location_name",
            validation
        );
        validateUniqueNames(
            workbook.activities().stream().collect(Collectors.groupingBy(activity -> normalizeKey(activity.activityName()))),
            "Activities.activity_name",
            "activity_name",
            validation
        );

        Map<String, List<EventImportWorkbook.ShiftRow>> shiftsByPlanAndName = workbook.shifts().stream()
            .collect(Collectors.groupingBy(shift -> normalizeKey(shift.shiftPlanName()) + "||" + normalizeKey(shift.shiftName())));
        for (var entry : shiftsByPlanAndName.entrySet()) {
            List<EventImportWorkbook.ShiftRow> rows = entry.getValue();
            if (rows.size() <= 1) {
                continue;
            }

            EventImportWorkbook.ShiftRow first = rows.get(0);
            String rowNumbers = rows.stream()
                .map(row -> String.valueOf(row.excelRow()))
                .collect(Collectors.joining(", "));

            validation.error(
                "Shifts.shift_name",
                "Duplicate shift_name '" + first.shiftName() + "' in shift_plan '" + first.shiftPlanName() + "' (rows: " + rowNumbers + ")."
            );
        }

        Set<String> knownLocations = workbook.locations().stream()
            .map(EventImportWorkbook.LocationRow::locationName)
            .filter(StringUtils::isNotBlank)
            .map(EventImportWorkbookParser::normalizeKey)
            .collect(Collectors.toSet());
        Set<String> knownActivities = workbook.activities().stream()
            .map(EventImportWorkbook.ActivityRow::activityName)
            .filter(StringUtils::isNotBlank)
            .map(EventImportWorkbookParser::normalizeKey)
            .collect(Collectors.toSet());

        for (EventImportWorkbook.ActivityRow activity : workbook.activities()) {
            if (activity.start() != null && activity.end() != null && !activity.start().isBefore(activity.end())) {
                validation.error(
                    "Activities.start_time_utc",
                    "Sheet 'Activities', row " + activity.excelRow() + ": start_time_utc must be before end_time_utc."
                );
            }

            if (eventRow.start() != null && activity.start() != null && activity.start().isBefore(eventRow.start())) {
                validation.error(
                    "Activities.start_time_utc",
                    "Sheet 'Activities', row " + activity.excelRow() + ": activity starts before event start."
                );
            }
            if (eventRow.end() != null && activity.end() != null && activity.end().isAfter(eventRow.end())) {
                validation.error(
                    "Activities.end_time_utc",
                    "Sheet 'Activities', row " + activity.excelRow() + ": activity ends after event end."
                );
            }

            if (StringUtils.isNotBlank(activity.locationName()) && !knownLocations.contains(normalizeKey(activity.locationName()))) {
                validation.error(
                    "Activities.location_name",
                    "Sheet 'Activities', row " + activity.excelRow() + ": Unknown location_name '" + activity.locationName() + "'."
                );
            }
        }

        for (EventImportWorkbook.ShiftRow shift : workbook.shifts()) {
            if (shift.start() != null && shift.end() != null && !shift.start().isBefore(shift.end())) {
                validation.error(
                    "Shifts.start_time_utc",
                    "Sheet 'Shifts', row " + shift.excelRow() + ": start_time_utc must be before end_time_utc."
                );
            }

            if (StringUtils.isNotBlank(shift.locationName()) && StringUtils.isNotBlank(shift.activityName())) {
                validation.error(
                    "Shifts.location_name",
                    "Sheet 'Shifts', row " + shift.excelRow() + ": Cannot set both location_name and activity_name."
                );
            }

            if (StringUtils.isNotBlank(shift.locationName()) && !knownLocations.contains(normalizeKey(shift.locationName()))) {
                validation.error(
                    "Shifts.location_name",
                    "Sheet 'Shifts', row " + shift.excelRow() + ": Unknown location_name '" + shift.locationName() + "'."
                );
            }

            if (StringUtils.isNotBlank(shift.activityName()) && !knownActivities.contains(normalizeKey(shift.activityName()))) {
                validation.error(
                    "Shifts.activity_name",
                    "Sheet 'Shifts', row " + shift.excelRow() + ": Unknown activity_name '" + shift.activityName() + "'."
                );
            }

            if (eventRow.start() != null && shift.start() != null && shift.start().isBefore(eventRow.start())) {
                validation.error(
                    "Shifts.start_time_utc",
                    "Sheet 'Shifts', row " + shift.excelRow() + ": shift starts before event start."
                );
            }
            if (eventRow.end() != null && shift.end() != null && shift.end().isAfter(eventRow.end())) {
                validation.error(
                    "Shifts.end_time_utc",
                    "Sheet 'Shifts', row " + shift.excelRow() + ": shift ends after event end."
                );
            }
        }
    }

    private <T> void validateUniqueNames(Map<String, List<T>> rowsByKey, String field, String displayName, ValidationException.Builder validation) {
        for (List<T> rows : rowsByKey.values()) {
            if (rows.size() <= 1) {
                continue;
            }

            if (rows.get(0) instanceof EventImportWorkbook.LocationRow locationRow) {
                String rowNumbers = rows.stream()
                    .map(EventImportWorkbook.LocationRow.class::cast)
                    .map(row -> String.valueOf(row.excelRow()))
                    .collect(Collectors.joining(", "));
                validation.error(field, "Duplicate " + displayName + " '" + locationRow.locationName() + "' (rows: " + rowNumbers + ").");
            }

            if (rows.get(0) instanceof EventImportWorkbook.ActivityRow activityRow) {
                String rowNumbers = rows.stream()
                    .map(EventImportWorkbook.ActivityRow.class::cast)
                    .map(row -> String.valueOf(row.excelRow()))
                    .collect(Collectors.joining(", "));
                validation.error(field, "Duplicate " + displayName + " '" + activityRow.activityName() + "' (rows: " + rowNumbers + ").");
            }
        }
    }

    private Map<String, Integer> readHeader(Sheet sheet, int headerRowIndex) {
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            throw ValidationException.builder()
                .context(VALIDATION_CONTEXT)
                .error("file", "Sheet '" + sheet.getSheetName() + "' is missing header row at index " + (headerRowIndex + 1) + ".")
                .build();
        }

        Map<String, Integer> header = new HashMap<>();
        for (Cell cell : headerRow) {
            String raw = cell.getCellType() == CellType.STRING ? cell.getStringCellValue() : String.valueOf(cell);
            String key = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            if (!key.isBlank()) {
                header.put(key, cell.getColumnIndex());
            }
        }
        return header;
    }

    private String readStringCell(Sheet sheet, int rowIndex, Map<String, Integer> header, String column, ValidationException.Builder validation,
                                  boolean required) {
        Integer columnIndex = header.get(column);
        if (columnIndex == null) {
            validation.error("file", "Missing required column '" + column + "' in sheet '" + sheet.getSheetName() + "'.");
            return null;
        }

        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            if (required) {
                validation.error("file", "Sheet '" + sheet.getSheetName() + "', row " + (rowIndex + 1) + ": '" + column + "' is required.");
            }
            return null;
        }

        String value = cellToString(row.getCell(columnIndex));
        if (required && StringUtils.isBlank(value)) {
            validation.error("file", "Sheet '" + sheet.getSheetName() + "', row " + (rowIndex + 1) + ": '" + column + "' is required.");
        }

        return value == null ? null : value.trim();
    }

    private Instant readInstantCell(Sheet sheet, int rowIndex, Map<String, Integer> header, String column, ValidationException.Builder validation,
                                    boolean required) {
        String value = readStringCell(sheet, rowIndex, header, column, validation, required);
        if (StringUtils.isBlank(value)) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z")) {
            normalized = normalized.replace("Z", ":00Z");
        }
        if (normalized.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) {
            normalized = normalized + ":00Z";
        }

        try {
            return Instant.parse(normalized);
        } catch (Exception e) {
            validation.error(
                "file",
                "Sheet '" + sheet.getSheetName() + "', row " + (rowIndex + 1) + ": '" + column
                    + "' has invalid datetime format: '" + normalized + "'. Expected ISO 8601 format."
            );
            return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (int columnIndex = row.getFirstCellNum(); columnIndex < row.getLastCellNum(); columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = cellToString(cell);
                if (StringUtils.isNotBlank(value)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String cellToString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                ? cell.getLocalDateTimeCellValue().toString()
                : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
