package at.control.shiftcontrol.shift.service.event.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import at.control.shiftcontrol.shift.entity.Activity;
import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.Location;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.lib.event.events.EventEvent;
import at.control.shiftcontrol.platform.exception.ValidationException;
import at.control.shiftcontrol.shift.lib.type.LockStatus;
import at.control.shiftcontrol.identity.AdminOnly;
import at.control.shiftcontrol.shift.dao.ActivityDao;
import at.control.shiftcontrol.shift.dao.EventDao;
import at.control.shiftcontrol.shift.dao.LocationDao;
import at.control.shiftcontrol.shift.dao.ShiftDao;
import at.control.shiftcontrol.shift.dao.ShiftPlanDao;
import at.control.shiftcontrol.shift.dto.event.EventExportDto;
import at.control.shiftcontrol.shift.dto.event.EventImportResultDto;
import at.control.shiftcontrol.shift.mapper.EventMapper;
import at.control.shiftcontrol.shift.mapper.ShiftAssemblingMapper;
import at.control.shiftcontrol.shift.service.event.EventImportService;
import at.control.shiftcontrol.shift.service.event.impl.importer.EventImportWorkbook;
import at.control.shiftcontrol.shift.service.event.impl.importer.EventImportWorkbookParser;

@Service
@RequiredArgsConstructor
public class EventImportServiceImpl implements EventImportService {
    private static final LockStatus DEFAULT_LOCK_STATUS = LockStatus.SELF_SIGNUP;
    private static final int DEFAULT_NO_ROLE_POINTS_PER_MIN = 0;
    private static final int DEFAULT_SHIFT_BONUS_REWARD_POINTS = 0;

    private final EventDao eventDao;
    private final ShiftPlanDao shiftPlanDao;
    private final ShiftDao shiftDao;
    private final LocationDao locationDao;
    private final ActivityDao activityDao;
    private final ShiftAssemblingMapper shiftMapper;
    private final EventImportWorkbookParser workbookParser;
    private final ApplicationEventPublisher publisher;

    @Override
    @AdminOnly
    @Transactional
    public EventImportResultDto importEvent(@NonNull MultipartFile file) {
        ValidationException.Builder validation = ValidationException.builder()
            .context(EventImportWorkbookParser.VALIDATION_CONTEXT);

        if (file.isEmpty()) {
            validation.error("file", "Uploaded file is empty.");
        }

        if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".xlsx")) {
            validation.error("file", "Invalid file type. Please upload an Excel .xlsx file.");
        }

        validation.throwIfInvalid();

        EventImportWorkbook workbook;
        try (InputStream inputStream = file.getInputStream(); Workbook poiWorkbook = new XSSFWorkbook(inputStream)) {
            workbook = workbookParser.parse(poiWorkbook);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw ValidationException.builder()
                .context(EventImportWorkbookParser.VALIDATION_CONTEXT)
                .error("file", "Failed to read Excel file: " + e.getMessage())
                .build();
        }

        if (eventDao.existsByNameIgnoreCase(workbook.eventRow().eventName())) {
            throw ValidationException.builder()
                .context(EventImportWorkbookParser.VALIDATION_CONTEXT)
                .error("Event.event_name", "Event name already exists: '" + workbook.eventRow().eventName() + "'.")
                .build();
        }

        Event event = createEvent(workbook.eventRow());
        Map<String, Location> locationsByName = createLocations(event, workbook.locations());
        Map<String, Activity> activitiesByName = createActivities(event, workbook.activities(), locationsByName);
        Map<String, ShiftPlan> shiftPlansByName = createShiftPlans(event, workbook.shifts());
        List<Shift> shifts = createShifts(workbook.shifts(), shiftPlansByName, locationsByName, activitiesByName);

        shifts = new ArrayList<>(shiftDao.saveAll(shifts));

        publisher.publishEvent(EventEvent.eventImported(event));
        return EventImportResultDto.builder()
            .event(EventMapper.toEventDto(event))
            .shifts(shiftMapper.assemble(shifts))
            .build();
    }

    private Event createEvent(EventImportWorkbook.EventRow eventRow) {
        Event event = Event.builder()
            .name(eventRow.eventName())
            .startTime(eventRow.start())
            .endTime(eventRow.end())
            .shortDescription(eventRow.shortDescription())
            .longDescription(eventRow.longDescription())
            .build();

        return eventDao.save(event);
    }

    private Map<String, Location> createLocations(Event event, List<EventImportWorkbook.LocationRow> locationRows) {
        Map<String, Location> locationsByName = new LinkedHashMap<>();

        for (EventImportWorkbook.LocationRow locationRow : locationRows) {
            Location location = Location.builder()
                .event(event)
                .name(locationRow.locationName())
                .description(locationRow.description())
                .url(locationRow.url())
                .readOnly(false)
                .build();

            location = locationDao.save(location);
            locationsByName.put(normalizeKey(locationRow.locationName()), location);
        }

        return locationsByName;
    }

    private Map<String, Activity> createActivities(Event event, List<EventImportWorkbook.ActivityRow> activityRows,
                                                   Map<String, Location> locationsByName) {
        Map<String, Activity> activitiesByName = new LinkedHashMap<>();

        for (EventImportWorkbook.ActivityRow activityRow : activityRows) {
            Location location = resolveLocation(activityRow.locationName(), locationsByName);

            Activity activity = Activity.builder()
                .event(event)
                .name(activityRow.activityName())
                .description(activityRow.description())
                .startTime(activityRow.start())
                .endTime(activityRow.end())
                .location(location)
                .readOnly(false)
                .build();

            activity = activityDao.save(activity);
            activitiesByName.put(normalizeKey(activityRow.activityName()), activity);
        }

        return activitiesByName;
    }

    private Map<String, ShiftPlan> createShiftPlans(Event event, List<EventImportWorkbook.ShiftRow> shiftRows) {
        Map<String, ShiftPlan> shiftPlansByName = new LinkedHashMap<>();

        for (EventImportWorkbook.ShiftRow shiftRow : shiftRows) {
            String normalizedPlanName = normalizeKey(shiftRow.shiftPlanName());
            if (shiftPlansByName.containsKey(normalizedPlanName)) {
                continue;
            }

            ShiftPlan shiftPlan = ShiftPlan.builder()
                .event(event)
                .name(shiftRow.shiftPlanName())
                .lockStatus(DEFAULT_LOCK_STATUS)
                .defaultNoRolePointsPerMinute(DEFAULT_NO_ROLE_POINTS_PER_MIN)
                .build();

            shiftPlan = shiftPlanDao.save(shiftPlan);
            shiftPlansByName.put(normalizedPlanName, shiftPlan);
        }

        return shiftPlansByName;
    }

    private List<Shift> createShifts(List<EventImportWorkbook.ShiftRow> shiftRows, Map<String, ShiftPlan> shiftPlansByName,
                                     Map<String, Location> locationsByName, Map<String, Activity> activitiesByName) {
        List<Shift> shifts = new ArrayList<>(shiftRows.size());

        for (EventImportWorkbook.ShiftRow shiftRow : shiftRows) {
            Activity activity = resolveActivity(shiftRow.activityName(), activitiesByName);
            Location location = activity != null ? activity.getLocation() : resolveLocation(shiftRow.locationName(), locationsByName);

            shifts.add(Shift.builder()
                .shiftPlan(shiftPlansByName.get(normalizeKey(shiftRow.shiftPlanName())))
                .name(shiftRow.shiftName())
                .startTime(shiftRow.start())
                .endTime(shiftRow.end())
                .shortDescription(shiftRow.shortDescription())
                .longDescription(shiftRow.longDescription())
                .location(location)
                .relatedActivity(activity)
                .bonusRewardPoints(DEFAULT_SHIFT_BONUS_REWARD_POINTS)
                .build());
        }

        return shifts;
    }

    private Location resolveLocation(String locationName, Map<String, Location> locationsByName) {
        if (StringUtils.isBlank(locationName)) {
            return null;
        }
        return locationsByName.get(normalizeKey(locationName));
    }

    private Activity resolveActivity(String activityName, Map<String, Activity> activitiesByName) {
        if (StringUtils.isBlank(activityName)) {
            return null;
        }
        return activitiesByName.get(normalizeKey(activityName));
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    @Override
    public EventExportDto getEventImportTemplate() {
        String mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        try {
            InputStream templateStream = this.getClass().getResourceAsStream("/templates/event_import_template.xlsx");
            if (templateStream == null) {
                throw new FileNotFoundException("Event import template not found.");
            }

            return EventExportDto.builder()
                .exportStream(templateStream)
                .fileName("event_import_template.xlsx")
                .mediaType(MediaType.parseMediaType(mediaType))
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load event import template: " + e.getMessage(), e);
        }
    }
}
