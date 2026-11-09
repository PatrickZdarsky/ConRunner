package at.control.shiftcontrol.shift.endpoint.event;

import java.time.LocalDate;
import java.util.Collection;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.lib.type.ShiftRelevance;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dto.event.schedule.ActivityScheduleDaySearchDto;
import at.control.shiftcontrol.shift.dto.event.schedule.ActivityScheduleDto;
import at.control.shiftcontrol.shift.dto.event.schedule.EventScheduleContentDto;
import at.control.shiftcontrol.shift.dto.event.schedule.EventScheduleDaySearchDto;
import at.control.shiftcontrol.shift.dto.event.schedule.EventScheduleFilterDto;
import at.control.shiftcontrol.shift.dto.event.schedule.EventScheduleFilterValuesDto;
import at.control.shiftcontrol.shift.dto.event.schedule.EventScheduleLayoutDto;
import at.control.shiftcontrol.shift.service.event.EventScheduleService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/events/{eventId}/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EventScheduleEndpoint {
    private final EventScheduleService eventScheduleService;

    @GetMapping("/layout")
    @Operation(
        operationId = "getEventScheduleLayout",
        description = "Get (volunteer related) schedule layout data for a specific event"
    )
    public EventScheduleLayoutDto getEventScheduleLayout(
        @PathVariable String eventId,
        @RequestParam(required = false) Collection<String> shiftPlanIds,
        @RequestParam(required = false) String shiftName,
        @RequestParam(required = false) Collection<ShiftRelevance> shiftRelevances,
        @RequestParam(required = false) Collection<String> roleIds,
        @RequestParam(required = false) Collection<String> locationIds
    ) {
        var filterDto = new EventScheduleFilterDto();
        filterDto.setShiftPlanIds(shiftPlanIds);
        filterDto.setShiftName(shiftName);
        filterDto.setShiftRelevances(shiftRelevances);
        filterDto.setRoleIds(roleIds);
        filterDto.setLocationIds(locationIds);
        return eventScheduleService.getEventScheduleLayout(ConvertUtil.idToLong(eventId), filterDto);
    }

    @GetMapping("/content")
    @Operation(
        operationId = "getEventScheduleContent",
        description = "Get (volunteer related) schedule content data for a specific day of a specific event"
    )
    public EventScheduleContentDto getEventScheduleContent(
        @PathVariable String eventId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) Collection<String> shiftPlanIds,
        @RequestParam(required = false) String shiftName,
        @RequestParam(required = false) Collection<ShiftRelevance> shiftRelevances,
        @RequestParam(required = false) Collection<String> roleIds,
        @RequestParam(required = false) Collection<String> locationIds
    ) {
        var eventScheduleDaySearchDto = new EventScheduleDaySearchDto(date);
        eventScheduleDaySearchDto.setShiftPlanIds(shiftPlanIds);
        eventScheduleDaySearchDto.setShiftName(shiftName);
        eventScheduleDaySearchDto.setShiftRelevances(shiftRelevances);
        eventScheduleDaySearchDto.setRoleIds(roleIds);
        eventScheduleDaySearchDto.setLocationIds(locationIds);
        return eventScheduleService.getEventScheduleContent(ConvertUtil.idToLong(eventId), eventScheduleDaySearchDto);
    }

    @GetMapping("/filters")
    @Operation(
        operationId = "getEventScheduleFilterValues",
        description = "Get available filter values for the schedule of a specific event"
    )
    public EventScheduleFilterValuesDto getEventScheduleFilterValues(@PathVariable String eventId) {
        return eventScheduleService.getEventScheduleFilterValues(ConvertUtil.idToLong(eventId));
    }

    @GetMapping("/activities")
    @Operation(
        operationId = "getActivitySchedule",
        description = "Get the activity schedule of an event"
    )
    public ActivityScheduleDto getActivitySchedule(
        @PathVariable String eventId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        var searchDto = ActivityScheduleDaySearchDto.builder().date(date).build();
        return eventScheduleService.getActivityScheduleOfEvent(ConvertUtil.idToLong(eventId), searchDto);
    }
}
