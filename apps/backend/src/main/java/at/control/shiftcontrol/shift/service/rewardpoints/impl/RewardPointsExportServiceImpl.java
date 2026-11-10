package at.control.shiftcontrol.shift.service.rewardpoints.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import at.control.shiftcontrol.shift.util.UniqueCodeGenerator;
import at.control.shiftcontrol.platform.exception.FileExportException;
import at.control.shiftcontrol.identity.AdminOnly;
import at.control.shiftcontrol.shift.dao.EventDao;
import at.control.shiftcontrol.shift.dao.RewardPointsShareTokenDao;
import at.control.shiftcontrol.shift.dao.RewardPointsTransactionDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.event.EventExportDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsExportDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.VolunteerPointsDto;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsCalculator;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsExportService;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsLedgerService;
import at.control.shiftcontrol.shift.userdirectory.UserLookupService;

@Service
public class RewardPointsExportServiceImpl extends RewardPointsServiceImpl implements RewardPointsExportService {
    public RewardPointsExportServiceImpl(RewardPointsCalculator calculator,
                                         RewardPointsLedgerService ledgerService,
                                         RewardPointsShareTokenDao rewardPointsShareTokenDao,
                                         EventDao eventDao, VolunteerDao volunteerDao,
                                         RewardPointsTransactionDao rewardPointsTransactionDao,
                                         UserLookupService userDirectoryService,
                                         ApplicationEventPublisher publisher,
                                         UniqueCodeGenerator uniqueCodeGenerator) {
        super(calculator, ledgerService, rewardPointsShareTokenDao, eventDao, volunteerDao, rewardPointsTransactionDao, userDirectoryService, publisher,
            uniqueCodeGenerator);
    }

    @Override
    @AdminOnly
    public EventExportDto exportRewardPoints() {
        var mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        Collection<RewardPointsExportDto> export = getRewardPointsForAllUsersOverAllEvents();

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100); // keep 100 rows in memory
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            wb.setCompressTempFiles(true);

            CreationHelper helper = wb.getCreationHelper();
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle textStyle = createTextStyle(wb);
            CellStyle intStyle = createIntStyle(wb);
            CellStyle dateStyle = createDateTimeStyle(wb, helper);

            buildDetailsSheet(wb, export, headerStyle, textStyle, intStyle, dateStyle);
            buildSummarySheet(wb, export, headerStyle, textStyle, intStyle);

            wb.write(out);

            InputStream stream = new ByteArrayInputStream(out.toByteArray());

            return EventExportDto.builder()
                .exportStream(stream)
                .fileName("reward-points-export.xlsx")
                .mediaType(MediaType.parseMediaType(mediaType))
                .build();

        } catch (IOException e) {
            throw new FileExportException("Failed to export reward points", e);
        }
    }

    private void buildDetailsSheet(
        Workbook wb,
        Collection<RewardPointsExportDto> export,
        CellStyle headerStyle,
        CellStyle textStyle,
        CellStyle intStyle,
        CellStyle dateStyle
    ) {
        Sheet sheet = wb.createSheet("Details");
        int r = 0;

        Row header = sheet.createRow(r++);
        String[] cols = {
            "Event ID", "Event Name", "Finished",
            "Start", "End",
            "Volunteer ID", "First Name", "Last Name", "Email",
            "Points"
        };
        for (int i = 0; i < cols.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(headerStyle);
        }

        for (RewardPointsExportDto e : export) {
            var event = e.getEvent();

            for (VolunteerPointsDto vp : e.getVolunteerPoints()) {
                Row row = sheet.createRow(r++);

                int c = 0;
                createTextCell(row, c++, event.getId(), textStyle);
                createTextCell(row, c++, event.getName(), textStyle);

                Cell finishedCell = row.createCell(c++);
                finishedCell.setCellValue(e.isEventFinished());
                finishedCell.setCellStyle(textStyle);

                createDateCell(row, c++, event.getStartTime(), dateStyle);
                createDateCell(row, c++, event.getEndTime(), dateStyle);

                createTextCell(row, c++, vp.getVolunteerId(), textStyle);
                createTextCell(row, c++, vp.getFirstName(), textStyle);
                createTextCell(row, c++, vp.getLastName(), textStyle);
                createTextCell(row, c++, vp.getEmail(), textStyle);

                Cell pointsCell = row.createCell(c);
                pointsCell.setCellValue(vp.getRewardPoints());
                pointsCell.setCellStyle(intStyle);
            }
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, cols.length - 1));

        int[] widths = {14, 30, 10, 18, 18, 40, 16, 16, 32, 10};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    private void buildSummarySheet(
        Workbook wb,
        Collection<RewardPointsExportDto> export,
        CellStyle headerStyle,
        CellStyle textStyle,
        CellStyle intStyle
    ) {
        Sheet sheet = wb.createSheet("Summary");

        List<RewardPointsExportDto> events = export.stream()
            .sorted(Comparator.comparing(e -> e.getEvent().getStartTime()))
            .toList();

        class VolRow {
            String volunteerId, firstName, lastName, email;
            final Map<String, Integer> pointsByEventId = new HashMap<>();
            int totalFinished = 0;
            int totalAll = 0;
        }

        Map<String, VolRow> vols = new HashMap<>();

        for (RewardPointsExportDto e : events) {
            String eventId = e.getEvent().getId();

            for (VolunteerPointsDto vp : e.getVolunteerPoints()) {
                VolRow vr = vols.computeIfAbsent(vp.getVolunteerId(), id -> {
                    VolRow x = new VolRow();
                    x.volunteerId = id;
                    x.firstName = vp.getFirstName();
                    x.lastName = vp.getLastName();
                    x.email = vp.getEmail();
                    return x;
                });

                int pts = vp.getRewardPoints();
                vr.pointsByEventId.put(eventId, pts);
                vr.totalAll += pts;
                if (e.isEventFinished()) {
                    vr.totalFinished += pts;
                }
            }
        }

        int r = 0;
        Row header = sheet.createRow(r++);
        int c = 0;

        String[] fixed = {"Volunteer ID", "First Name", "Last Name", "Email", "Total (Finished)", "Total (All)"};
        for (String col : fixed) {
            Cell cell = header.createCell(c++);
            cell.setCellValue(col);
            cell.setCellStyle(headerStyle);
        }

        for (RewardPointsExportDto e : events) {
            Cell cell = header.createCell(c++);
            cell.setCellValue(e.getEvent().getName());
            cell.setCellStyle(headerStyle);
        }

        List<VolRow> sortedVols = vols.values().stream()
            .sorted(Comparator.comparing((VolRow v) -> v.lastName == null ? "" : v.lastName)
                .thenComparing(v -> v.firstName == null ? "" : v.firstName))
            .toList();

        for (VolRow v : sortedVols) {
            Row row = sheet.createRow(r++);
            int cc = 0;

            createTextCell(row, cc++, v.volunteerId, textStyle);
            createTextCell(row, cc++, v.firstName, textStyle);
            createTextCell(row, cc++, v.lastName, textStyle);
            createTextCell(row, cc++, v.email, textStyle);

            Cell finishedTotal = row.createCell(cc++);
            finishedTotal.setCellValue(v.totalFinished);
            finishedTotal.setCellStyle(intStyle);

            Cell allTotal = row.createCell(cc++);
            allTotal.setCellValue(v.totalAll);
            allTotal.setCellStyle(intStyle);

            for (RewardPointsExportDto e : events) {
                Integer pts = v.pointsByEventId.get(e.getEvent().getId());
                Cell p = row.createCell(cc++);
                if (pts != null) {
                    p.setCellValue(pts);
                    p.setCellStyle(intStyle);
                } else {
                    p.setCellValue(0);
                    p.setCellStyle(intStyle);
                }
            }
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, fixed.length + events.size() - 1));

        sheet.setColumnWidth(0, 40 * 256);
        sheet.setColumnWidth(1, 16 * 256);
        sheet.setColumnWidth(2, 16 * 256);
        sheet.setColumnWidth(3, 32 * 256);
        sheet.setColumnWidth(4, 16 * 256);
        sheet.setColumnWidth(5, 12 * 256);
        // event columns
        for (int i = 0; i < events.size(); i++) {
            sheet.setColumnWidth(fixed.length + i, 18 * 256);
        }
    }

    private static void createTextCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void createDateCell(Row row, int col, Instant instant, CellStyle style) {
        Cell cell = row.createCell(col);
        if (instant != null) {
            cell.setCellValue(Date.from(instant));
        } else {
            cell.setBlank();
        }
        cell.setCellStyle(style);
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);

        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createTextStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        return style;
    }

    private static CellStyle createIntStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("0"));
        return style;
    }

    private static CellStyle createDateTimeStyle(Workbook wb, CreationHelper helper) {
        CellStyle style = wb.createCellStyle();
        // Excel datetime format
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
        return style;
    }
}


