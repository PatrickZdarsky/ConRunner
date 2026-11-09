package at.control.shiftcontrol.shift.service.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Stream;

import at.shiftcontrol.lib.entity.Activity;
import at.shiftcontrol.lib.entity.Location;
import at.shiftcontrol.lib.event.events.ActivityEvent;
import at.shiftcontrol.lib.exception.BadRequestException;
import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.dao.ActivityDao;
import at.shiftcontrol.shiftservice.dao.EventDao;
import at.shiftcontrol.shiftservice.dao.LocationDao;
import at.shiftcontrol.shiftservice.dto.activity.ActivityDto;
import at.shiftcontrol.shiftservice.dto.activity.ActivityModificationDto;
import at.shiftcontrol.shiftservice.dto.activity.ActivitySuggestionDto;
import at.shiftcontrol.shiftservice.dto.activity.ActivityTimeFilterDto;
import at.shiftcontrol.shiftservice.mapper.ActivityMapper;
import at.shiftcontrol.shiftservice.service.ActivityService;
import at.shiftcontrol.shiftservice.util.SecurityHelper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    private final EventDao eventDao;
    private final ActivityDao activityDao;
    private final LocationDao locationDao;
    private final ApplicationEventPublisher publisher;
    private final SecurityHelper securityHelper;

    @Override
    public @org.jspecify.annotations.NonNull ActivityDto getActivity(long activityId) {
        var activity = activityDao.getById(activityId);

        return ActivityMapper.toActivityDto(activity);
    }

    @Override
    public @org.jspecify.annotations.NonNull Collection<ActivityDto> getActivitiesForEvent(long eventId) {
        var event = eventDao.getById(eventId);
        securityHelper.assertUserIsPlannerInAnyPlanOfEvent(event);

        var activities = activityDao.findAllByEventId(eventId);

        return activities.stream()
            .map(ActivityMapper::toActivityDto)
            .toList();
    }

    @Override
    @AdminOnly
    public @org.jspecify.annotations.NonNull ActivityDto createActivity(long eventId, @NonNull ActivityModificationDto modificationDto) {
        var event = eventDao.getById(eventId);

        var newActivity = Activity.builder()
            .event(event)
            .readOnly(false)
            .build();

        //VALIDATION
        validateNameUniquenessInEvent(eventId, modificationDto.getName(), null);
        var activity = validateModificationDtoAndSetActivityFields(modificationDto, newActivity);

        //ACT
        activity = activityDao.save(activity);

        publisher.publishEvent(ActivityEvent.activityCreated(activity));
        return ActivityMapper.toActivityDto(activity);
    }

    @Override
    @AdminOnly
    public @org.jspecify.annotations.NonNull ActivityDto updateActivity(long activityId, @NonNull ActivityModificationDto modificationDto) {
        var activity = activityDao.getById(activityId);

        //VALIDATION
        validateNameUniquenessInEvent(activity.getEvent().getId(), modificationDto.getName(), activityId);
        activity = validateModificationDtoAndSetActivityFields(modificationDto, activity);

        activity = activityDao.save(activity);

        publisher.publishEvent(ActivityEvent.activityUpdated(activity));
        return ActivityMapper.toActivityDto(activity);
    }

    Activity validateModificationDtoAndSetActivityFields(ActivityModificationDto modificationDto, Activity activity) {
        if (modificationDto.getEndTime().isBefore(modificationDto.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        if (activity.isReadOnly()) {
            throw new BadRequestException("Cannot modify read-only activity");
        }

        if (modificationDto.getStartTime().isBefore(activity.getEvent().getStartTime())
            || modificationDto.getEndTime().isAfter(activity.getEvent().getEndTime())) {
            throw new BadRequestException("Activity time range must be within event time range");
        }

        Location location = null;
        if (StringUtils.isNotBlank(modificationDto.getLocationId())) {
            location = locationDao.getById(ConvertUtil.idToLong(modificationDto.getLocationId()));
            if (location.isReadOnly()) {
                throw new BadRequestException("Cannot assign read-only location to activity");
            }
        }

        return ActivityMapper.updateActivity(modificationDto, location, activity);
    }

    void validateNameUniquenessInEvent(long eventId, String name, Long excludeActivityId) {
        var activitiesInEvent = activityDao.findAllByEventId(eventId);
        boolean nameExists = activitiesInEvent.stream()
            .anyMatch(activity -> activity.getName() != null
                && activity.getName().equalsIgnoreCase(name)
                && (excludeActivityId == null || activity.getId() != excludeActivityId));
        if (nameExists) {
            throw new BadRequestException("An activity with the given name already exists in the event");
        }
    }

    @Override
    @AdminOnly
    public void deleteActivity(long activityId) {
        var activity = activityDao.getById(activityId);

        if (activity.isReadOnly()) {
            throw new BadRequestException("Cannot modify read-only activity");
        }

        var activityEvent = ActivityEvent.activityDeleted(activity);
        activityDao.delete(activity);
        publisher.publishEvent(activityEvent);
    }

    @Override
    public @NonNull Collection<ActivityDto> suggestActivitiesForShift(long eventId, @Nullable ActivitySuggestionDto suggestionDto) {
        var event = eventDao.getById(eventId);
        securityHelper.assertUserIsPlannerInAnyPlanOfEvent(event);

        var activitiesOfEvent = activityDao.findAllByEventId(eventId);

        if (suggestionDto == null || (suggestionDto.getName() == null && suggestionDto.getTimeFilter() == null)) {
            return activitiesOfEvent.stream()
                .map(ActivityMapper::toActivityDto)
                .toList();
        }

        return getActivityStream(suggestionDto, activitiesOfEvent)
            .map(ActivityMapper::toActivityDto)
            .toList();
    }

    private @NonNull Stream<Activity> getActivityStream(ActivitySuggestionDto suggestionDto, Collection<Activity> activitiesOfEvent) {
        var timeFilter = suggestionDto.getTimeFilter();
        Instant shiftStart = timeFilter != null ? timeFilter.getStartTime() : null;
        Instant shiftEnd = timeFilter != null ? timeFilter.getEndTime() : null;
        var filteredByTime = filterByTime(activitiesOfEvent, timeFilter, shiftStart, shiftEnd);

        return filterByName(filteredByTime, suggestionDto.getName());
    }

    private @NonNull Stream<Activity> filterByTime(Collection<Activity> activitiesOfEvent, ActivityTimeFilterDto timeFilter, Instant shiftStart,
                                                   Instant shiftEnd) {
        int toleranceMinutes = timeFilter != null ? timeFilter.getToleranceInMinutes() : 0;

        boolean applyTimeFilter = shiftStart != null && shiftEnd != null;

        if (applyTimeFilter && shiftEnd.isBefore(shiftStart)) {
            throw new BadRequestException("End time must be after start time in time filter");
        }

        if (!applyTimeFilter) {
            return activitiesOfEvent.stream();
        }

        // narrow down by start/end time
        // any intersection of suggestion times & activity times is valid
        return activitiesOfEvent.stream()
            .filter(activity -> {
                Instant activityStart = activity.getStartTime();
                Instant activityEnd = activity.getEndTime();
                return !(activityEnd.isBefore(shiftStart.minusSeconds(toleranceMinutes * 60L))
                    || activityStart.isAfter(shiftEnd.plusSeconds(toleranceMinutes * 60L)));
            });
    }

    private @NonNull Stream<Activity> filterByName(@NonNull Stream<Activity> filteredByTime, String name) {
        Stream<Activity> finalFiltered = filteredByTime;
        if (StringUtils.isNotBlank(name)) {
            String nameFilterLower = name.toLowerCase().trim();
            finalFiltered = filteredByTime.filter(activity ->
                activity.getName() != null && activity.getName().toLowerCase().contains(nameFilterLower)
            );
        }
        return finalFiltered;
    }
}
