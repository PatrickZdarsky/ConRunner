package at.conrunner.shift.service.event.impl.importer;

import at.conrunner.platform.exception.ValidationException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventImportWorkbookParserTest {
    private final EventImportWorkbookParser parser = new EventImportWorkbookParser();

    @Test
    void parse_rejectsShiftReferencingUnknownActivity() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            addSheet(workbook, "Event",
                new String[] { "event_name", "short_description", "long_description", "start_time_utc", "end_time_utc" },
                new String[] { "required", "optional", "optional", "required", "required" },
                new String[] { "ShiftControl Fest", "Short", "Long", "2026-05-12T07:00:00Z", "2026-05-12T22:00:00Z" }
            );
            addSheet(workbook, "Locations",
                new String[] { "location_name", "description", "url" },
                new String[] { "required", "optional", "optional" },
                new String[] { "Main Hall", "Center stage", "https://example.test/main-hall" }
            );
            addSheet(workbook, "Activities",
                new String[] { "activity_name", "description", "start_time_utc", "end_time_utc", "location_name" },
                new String[] { "required", "optional", "required", "required", "optional" },
                new String[] { "Opening", "Kickoff", "2026-05-12T08:00:00Z", "2026-05-12T09:00:00Z", "Main Hall" }
            );
            addSheet(workbook, "Shifts",
                new String[] { "shift_plan_name", "shift_name", "start_time_utc", "end_time_utc", "location_name", "activity_name",
                    "short_description", "long_description" },
                new String[] { "required", "required", "required", "required", "optional", "optional", "optional", "optional" },
                new String[] { "Crew", "Door Support", "2026-05-12T08:00:00Z", "2026-05-12T10:00:00Z", "", "Missing Activity",
                    "Welcome", "Check badges" }
            );

            assertThatThrownBy(() -> parser.parse(workbook))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unknown activity_name 'Missing Activity'");
        }
    }

    private void addSheet(Workbook workbook, String sheetName, String[] headers, String[] notes, String[] sampleRow) {
        var sheet = workbook.createSheet(sheetName);
        var headerRow = sheet.createRow(0);
        for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
            headerRow.createCell(columnIndex).setCellValue(headers[columnIndex]);
        }

        var noteRow = sheet.createRow(1);
        for (int columnIndex = 0; columnIndex < notes.length; columnIndex++) {
            noteRow.createCell(columnIndex).setCellValue(notes[columnIndex]);
        }

        var dataRow = sheet.createRow(2);
        for (int columnIndex = 0; columnIndex < sampleRow.length; columnIndex++) {
            dataRow.createCell(columnIndex).setCellValue(sampleRow[columnIndex]);
        }
    }
}
