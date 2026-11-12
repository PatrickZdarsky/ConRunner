package at.conrunner.shift.endpoint.event;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.dto.TimeConstraintCreateDto;
import at.conrunner.shift.dto.TimeConstraintDto;
import at.conrunner.shift.service.TimeConstraintService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/events/{eventId}/time-constraints", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TimeConstraintEndpoint {
    private final CurrentShiftActor currentActor;
    private final TimeConstraintService timeConstraintService;

    @GetMapping()
    @Operation(
        operationId = "getTimeConstraints",
        description = "Get time constraints of the current user"
    )
    public Collection<TimeConstraintDto> getTimeConstraints(@PathVariable String eventId) {
        return timeConstraintService.getTimeConstraints(currentActor.get().requireUserId(), ConvertUtil.idToLong(eventId));
    }

    @PostMapping()
    @Operation(
        operationId = "createTimeConstraint",
        description = "Create a new time constraint for the current user"
    )
    public TimeConstraintDto createTimeConstraint(@PathVariable String eventId, @RequestBody TimeConstraintCreateDto createDto) {
        return timeConstraintService.createTimeConstraint(
            createDto,
            currentActor.get().requireUserId(),
            ConvertUtil.idToLong(eventId)
        );
    }

    @DeleteMapping("/{timeConstraintId}")
    @Operation(
        operationId = "deleteTimeConstraint",
        description = "Delete an existing time constraint of the current user"
    )
    public void deleteTimeConstraint(@PathVariable String eventId, @PathVariable String timeConstraintId) {
        // Check that the time constraint belongs to the current user (throws NotFoundException if not)
        timeConstraintService.getTimeConstraints(currentActor.get().requireUserId(), ConvertUtil.idToLong(eventId));
        timeConstraintService.delete(ConvertUtil.idToLong(timeConstraintId));
    }
}
