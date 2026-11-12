package at.conrunner.shift.endpoint.event;

import java.util.Collection;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.dto.event.EventDto;
import at.conrunner.shift.dto.event.EventImportResultDto;
import at.conrunner.shift.dto.event.EventModificationDto;
import at.conrunner.shift.dto.event.EventShiftPlansOverviewDto;
import at.conrunner.shift.dto.event.EventsDashboardOverviewDto;
import at.conrunner.shift.dto.shiftplan.ShiftPlanContactInfoDto;
import at.conrunner.shift.service.DashboardService;
import at.conrunner.shift.service.event.EventCloneService;
import at.conrunner.shift.service.event.EventExportService;
import at.conrunner.shift.service.event.EventImportService;
import at.conrunner.shift.service.event.EventService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/events", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EventEndpoint {
    private final CurrentShiftActor currentActor;
    private final EventService eventService;
    private final EventCloneService eventCloneService;
    private final EventExportService eventExportService;
    private final EventImportService eventImportService;
    private final DashboardService dashboardService;

    @GetMapping("/{eventId}")
    @Operation(
        operationId = "getEventById",
        description = "Find event by id"
    )
    public EventDto getEvent(@PathVariable String eventId) {
        return eventService.getEvent(ConvertUtil.idToLong(eventId));
    }

    @GetMapping
    @Operation(
        operationId = "getAllEvents",
        description = "Find all (volunteer related) events"
    )
    public Collection<EventDto> getAllEvents() {
        return eventService.search(null);
    }
    //Todo: Add search capability in future

    @GetMapping("/open")
    @Operation(
        operationId = "getAllOpenEvents",
        description = "Find all events that are currently ongoing or upcoming"
    )
    public Collection<EventDto> getAllOpenEvents() {
        return eventService.getAllOpenEvents(currentActor.get().requireUserId());
    }

    @PostMapping
    @Operation(
        operationId = "createEvent",
        description = "Create a new event"
    )
    public EventDto createEvent(@RequestBody @Valid EventModificationDto modificationDto) {
        return eventService.createEvent(modificationDto);
    }

    @PutMapping("/{eventId}")
    @Operation(
        operationId = "updateEvent",
        description = "Update an existing event"
    )
    public EventDto updateEvent(@PathVariable String eventId, @RequestBody @Valid EventModificationDto modificationDto) {
        return eventService.updateEvent(ConvertUtil.idToLong(eventId), modificationDto);
    }

    @DeleteMapping("/{eventId}")
    @Operation(
        operationId = "deleteEvent",
        description = "Delete an existing event"
    )
    public void deleteEvent(@PathVariable String eventId) {
        eventService.deleteEvent(ConvertUtil.idToLong(eventId));
    }

    @PostMapping("/{eventId}/clone")
    @Operation(
        operationId = "cloneEvent",
        description = "Clone an existing event"
    )
    public EventDto cloneEvent(@PathVariable String eventId) {
        return eventCloneService.cloneEvent(ConvertUtil.idToLong(eventId));
    }

    @GetMapping("/{eventId}/shift-plans-overview")
    @Operation(
        operationId = "getShiftPlansOverviewOfEvent",
        description = "Get an overview of all (volunteer related) shift plans of an event including statistics and reward points for the current user"
    )
    public EventShiftPlansOverviewDto getAllShiftPlanOverviewsOfEvent(@PathVariable String eventId) {
        return eventService.getEventShiftPlansOverview(ConvertUtil.idToLong(eventId), currentActor.get().requireUserId());
    }

    @GetMapping("/{eventId}/dashboard")
    @Operation(
        operationId = "getEventsDashboard",
        description = "Get (volunteer related) dashboard data for all events"
    )
    public EventsDashboardOverviewDto getEventsDashboard(@PathVariable String eventId) {
        return dashboardService.getDashboardOverviewsOfAllShiftPlans(eventId, currentActor.get().requireUserId());
    }

    @GetMapping("/{eventId}/export")
    @Operation(
        operationId = "exportEventData",
        description = "Export event data for external use"
    )
    public ResponseEntity<Resource> exportEventData(@PathVariable String eventId, @RequestParam String format) {
        var export = eventExportService.exportEvent(ConvertUtil.idToLong(eventId), format);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + export.getFileName())
            // filename will be set in frontend regardless of this value because header value is not used, but it is good practice to set it here anyway
            .contentType(export.getMediaType())
            .body(new InputStreamResource(export.getExportStream()));
    }

    @PostMapping(
        value = "/import",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
        operationId = "importEventData",
        description = "Import event data from external source"
    )
    public EventImportResultDto importEventData(@RequestPart("file") @Schema(format = "binary") @NotNull MultipartFile file) {
        return eventImportService.importEvent(file);
    }

    @GetMapping("/import/template")
    @Operation(
        operationId = "downloadEventImportTemplate",
        description = "Download event import template file"
    )
    public ResponseEntity<Resource> downloadEventImportTemplate() {
        var template = eventImportService.getEventImportTemplate();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + template.getFileName())
            // filename will be set in frontend regardless of this value because header value is not used, but it is good practice to set it here anyway
            .contentType(template.getMediaType())
            .body(new InputStreamResource(template.getExportStream()));
    }

    @GetMapping("/{eventId}/contacts")
    @Operation(
        operationId = "getPlannerContacts",
        description = "Get contact information for all planners"
    )
    public Collection<ShiftPlanContactInfoDto> getPlannerContacts(@PathVariable String eventId) {
        return eventService.getPlannerContactInfo(
            ConvertUtil.idToLong(eventId),
            currentActor.get().requireUserId());
    }
}
