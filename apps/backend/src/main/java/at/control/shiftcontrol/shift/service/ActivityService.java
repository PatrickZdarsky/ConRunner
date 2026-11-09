package at.control.shiftcontrol.shift.service;

import java.util.Collection;

import at.control.shiftcontrol.shift.dto.activity.ActivityDto;
import at.control.shiftcontrol.shift.dto.activity.ActivityModificationDto;
import at.control.shiftcontrol.shift.dto.activity.ActivitySuggestionDto;

import lombok.NonNull;
import org.jspecify.annotations.Nullable;

public interface ActivityService {
    /**
     * Get activity by id
     * @param activityId the id of the activity
     * @return the activity dto
     * @throws at.control.shiftcontrol.shift.lib.exception.NotFoundException if the activity does not exist
     */
    @NonNull ActivityDto getActivity(long activityId);

    /**
     * Get all activities for an event
     * @param eventId the id of the event
     * @return collection of activity dtos
     * @throws at.control.shiftcontrol.shift.lib.exception.NotFoundException if the event does not exist
     * @throws at.control.shiftcontrol.shift.lib.exception.ForbiddenException if the user is not a planner in any plan of the event
     */
    @NonNull Collection<ActivityDto> getActivitiesForEvent(long eventId);

    /**
     * Create a new activity for an event
     * @param eventId the id of the event
     * @param modificationDto the modification dto
     * @return the created activity dto
     * @throws at.control.shiftcontrol.shift.lib.exception.NotFoundException if the event does not exist
     * @throws at.control.shiftcontrol.shift.lib.exception.ForbiddenException if the user is not an admin of the event
     * @throws at.control.shiftcontrol.shift.lib.exception.BadRequestException if the activity name is not unique within the event
     */
    @NonNull ActivityDto createActivity(long eventId, @NonNull ActivityModificationDto modificationDto);

    /**
     * Update an existing activity
     * @param activityId the id of the activity
     * @param modificationDto the modification dto
     * @return the updated activity dto
     * @throws at.control.shiftcontrol.shift.lib.exception.NotFoundException if the activity does not exist
     * @throws at.control.shiftcontrol.shift.lib.exception.ForbiddenException if the user is not an admin of the event
     * @throws at.control.shiftcontrol.shift.lib.exception.BadRequestException if the activity name is not unique within the event
     */
    @NonNull ActivityDto updateActivity(long activityId, @NonNull ActivityModificationDto modificationDto);

    /**
     * Delete an activity
     * @param activityId the id of the activity
     * @throws at.control.shiftcontrol.shift.lib.exception.NotFoundException if the activity does not exist
     * @throws at.control.shiftcontrol.shift.lib.exception.ForbiddenException if the user is not an admin of the event
     */
    void deleteActivity(long activityId);

    /**
     * Suggest activities for a shift based on the given criteria
     * @param eventId the id of the event
     * @param suggestionDto the suggestion criteria
     * @return collection of suggested activity dtos
     * @throws at.control.shiftcontrol.shift.lib.exception.NotFoundException if the event does not exist
     * @throws at.control.shiftcontrol.shift.lib.exception.ForbiddenException if the user is not a planner in any plan of the event
     */
    @NonNull Collection<ActivityDto> suggestActivitiesForShift(long eventId, @Nullable ActivitySuggestionDto suggestionDto);
}
