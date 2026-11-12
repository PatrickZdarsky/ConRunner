package at.conrunner.shift.dao;

import java.util.Collection;
import java.util.Optional;

import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.dto.event.schedule.ActivityScheduleDaySearchDto;

public interface ActivityDao extends BasicDao<Activity, Long> {
    Collection<Activity> findAllByLocationId(Long locationId);

    Collection<Activity> findAllByEventId(Long eventId);

    Collection<Activity> findAllWithoutLocationByEventId(Long eventId);

    Collection<Activity> searchActivitiesInEvent(Long eventId, ActivityScheduleDaySearchDto searchDto);

    Optional<Activity> findByEventAndName(Long eventId, String name);
}
