package at.conrunner.shift.mapper;

import java.util.Collection;

import lombok.NoArgsConstructor;

import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.dto.activity.ActivityDto;
import at.conrunner.shift.dto.activity.ActivityModificationDto;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ActivityMapper {
    public static ActivityDto toActivityDto(Activity activity) {
        return new ActivityDto(
            String.valueOf(activity.getId()),
            activity.getName(),
            activity.getDescription(),
            activity.getStartTime(),
            activity.getEndTime(),
            activity.getLocation() == null ? null : LocationMapper.toLocationDto(activity.getLocation()),
            activity.isReadOnly()
        );
    }

    public static Collection<ActivityDto> toActivityDto(Collection<Activity> activities) {
        return activities.stream()
            .map(ActivityMapper::toActivityDto)
            .toList();
    }

    public static Activity updateActivity(ActivityModificationDto modificationDto, Location location, Activity activity) {
        activity.setName(modificationDto.getName());
        activity.setDescription(modificationDto.getDescription());
        activity.setStartTime(modificationDto.getStartTime());
        activity.setEndTime(modificationDto.getEndTime());
        activity.setLocation(location);
        return activity;

    }
}
