package at.control.shiftcontrol.shift.endpoint.activity;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dto.activity.ActivityDto;
import at.control.shiftcontrol.shift.dto.activity.ActivityModificationDto;
import at.control.shiftcontrol.shift.dto.activity.ActivitySuggestionDto;
import at.control.shiftcontrol.shift.service.ActivityService;

@Tag(
    name = "activity-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/events/{eventId}/activities", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ActivityCollectionEndpoint {
    private final ActivityService activityService;

    @GetMapping()
    @Operation(
        operationId = "getActivitiesForEvent",
        description = "Get all activities for a specific event"
    )
    public Collection<ActivityDto> getActivitiesForEvent(@PathVariable String eventId) {
        return activityService.getActivitiesForEvent(ConvertUtil.idToLong(eventId));
    }

    @PostMapping()
    @Operation(
        operationId = "createActivity",
        description = "Create a new activity for a specific event"
    )
    public ActivityDto createActivity(@PathVariable String eventId, @RequestBody @Valid ActivityModificationDto modificationDto) {
        return activityService.createActivity(ConvertUtil.idToLong(eventId), modificationDto);
    }

    @PostMapping("/suggest")
    @Operation(
        operationId = "suggestActivitiesForShift",
        description = "Suggest activities for a specific shift based on its time and optional name filter"
    )
    public Collection<ActivityDto> suggestActivitiesForShift(@PathVariable String eventId, @RequestBody @Valid ActivitySuggestionDto suggestionDto) {
        return activityService.suggestActivitiesForShift(ConvertUtil.idToLong(eventId), suggestionDto);
    }
}
