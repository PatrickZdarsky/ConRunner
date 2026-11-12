package at.conrunner.shift.service.event.impl;

import at.conrunner.shift.audit.EventManagementAudit;
import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.dao.ActivityDao;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.LocationDao;
import at.conrunner.shift.dao.ShiftDao;
import at.conrunner.shift.dao.ShiftPlanDao;
import at.conrunner.shift.dto.event.EventImportResultDto;
import at.conrunner.shift.mapper.ShiftAssemblingMapper;
import at.conrunner.shift.service.event.impl.importer.EventImportWorkbookParser;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventImportServiceImplTest {
    @Mock
    private EventDao eventDao;

    @Mock
    private ShiftPlanDao shiftPlanDao;

    @Mock
    private ShiftDao shiftDao;

    @Mock
    private LocationDao locationDao;

    @Mock
    private ActivityDao activityDao;

    @Mock
    private ShiftAssemblingMapper shiftMapper;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private EventImportServiceImpl service;

    @Test
    void importEvent_createsLocationsActivitiesAndShiftsInOrder() throws Exception {
        EventImportWorkbookParser parser = new EventImportWorkbookParser();
        service = new EventImportServiceImpl(org.mockito.Mockito.mock(EventManagementAudit.class), eventDao, shiftPlanDao, shiftDao, locationDao, activityDao, shiftMapper, parser, publisher);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "event-import.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            createWorkbookBytes()
        );

        when(eventDao.existsByNameIgnoreCase("Spring Festival 2026")).thenReturn(false);
        when(eventDao.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(101L);
            return event;
        });
        when(locationDao.save(any(Location.class))).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            location.setId(location.getName().equals("Main Hall") ? 201L : 202L);
            return location;
        });
        when(activityDao.save(any(Activity.class))).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            activity.setId(301L);
            return activity;
        });
        when(shiftPlanDao.save(any(ShiftPlan.class))).thenAnswer(invocation -> {
            ShiftPlan shiftPlan = invocation.getArgument(0);
            shiftPlan.setId(401L);
            return shiftPlan;
        });
        when(shiftDao.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));
        when(shiftMapper.assemble(anyCollection())).thenReturn(List.of());

        EventImportResultDto result = service.importEvent(file);

        var callOrder = inOrder(eventDao, locationDao, activityDao, shiftPlanDao, shiftDao);
        callOrder.verify(eventDao).save(any(Event.class));
        callOrder.verify(locationDao, times(2)).save(any(Location.class));
        callOrder.verify(activityDao).save(any(Activity.class));
        callOrder.verify(shiftPlanDao).save(any(ShiftPlan.class));
        callOrder.verify(shiftDao).saveAll(anyCollection());

        @SuppressWarnings("unchecked")
        var shiftsCaptor = org.mockito.ArgumentCaptor.forClass((Class<java.util.Collection<Shift>>) (Class<?>) java.util.Collection.class);
        org.mockito.Mockito.verify(shiftDao).saveAll(shiftsCaptor.capture());
        List<Shift> persistedShifts = List.copyOf(shiftsCaptor.getValue());

        assertThat(persistedShifts).hasSize(2);
        assertThat(persistedShifts.get(0).getRelatedActivity()).isNotNull();
        assertThat(persistedShifts.get(0).getRelatedActivity().getName()).isEqualTo("Opening Ceremony");
        assertThat(persistedShifts.get(0).getLocation()).isEqualTo(persistedShifts.get(0).getRelatedActivity().getLocation());
        assertThat(persistedShifts.get(1).getRelatedActivity()).isNull();
        assertThat(persistedShifts.get(1).getLocation()).isNotNull();
        assertThat(persistedShifts.get(1).getLocation().getName()).isEqualTo("Info Desk");

        assertThat(result.getEvent().getId()).isEqualTo("101");
        assertThat(result.getEvent().getName()).isEqualTo("Spring Festival 2026");
    }

    private byte[] createWorkbookBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            addSheet(workbook, "Event",
                new String[] { "event_name", "short_description", "long_description", "start_time_utc", "end_time_utc" },
                new String[] { "required", "optional", "optional", "required", "required" },
                List.<String[]>of(new String[] { "Spring Festival 2026", "Community festival", "Full-day community event",
                    "2026-05-12T07:00:00Z", "2026-05-12T22:00:00Z" })
            );
            addSheet(workbook, "Locations",
                new String[] { "location_name", "description", "url" },
                new String[] { "required", "optional", "optional" },
                List.of(
                    new String[] { "Main Hall", "Center stage", "https://example.test/main-hall" },
                    new String[] { "Info Desk", "Volunteer check-in", "https://example.test/info-desk" }
                )
            );
            addSheet(workbook, "Activities",
                new String[] { "activity_name", "description", "start_time_utc", "end_time_utc", "location_name" },
                new String[] { "required", "optional", "required", "required", "optional" },
                List.<String[]>of(new String[] { "Opening Ceremony", "Main kickoff", "2026-05-12T08:00:00Z", "2026-05-12T09:30:00Z",
                    "Main Hall" })
            );
            addSheet(workbook, "Shifts",
                new String[] { "shift_plan_name", "shift_name", "start_time_utc", "end_time_utc", "location_name", "activity_name",
                    "short_description", "long_description" },
                new String[] { "required", "required", "required", "required", "optional", "optional", "optional", "optional" },
                List.of(
                    new String[] { "Operations", "Stage Support", "2026-05-12T08:00:00Z", "2026-05-12T10:00:00Z", "",
                        "Opening Ceremony", "Assist speakers", "Support the ceremony" },
                    new String[] { "Operations", "Front Desk", "2026-05-12T09:00:00Z", "2026-05-12T12:00:00Z", "Info Desk", "",
                        "Guide guests", "Handle questions and check-in" }
                )
            );

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void addSheet(Workbook workbook, String sheetName, String[] headers, String[] notes, List<String[]> rows) {
        var sheet = workbook.createSheet(sheetName);
        var headerRow = sheet.createRow(0);
        for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
            headerRow.createCell(columnIndex).setCellValue(headers[columnIndex]);
        }

        var noteRow = sheet.createRow(1);
        for (int columnIndex = 0; columnIndex < notes.length; columnIndex++) {
            noteRow.createCell(columnIndex).setCellValue(notes[columnIndex]);
        }

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            var row = sheet.createRow(rowIndex + 2);
            String[] values = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
                row.createCell(columnIndex).setCellValue(values[columnIndex]);
            }
        }
    }
}
