package at.control.shiftcontrol.shift.dao.impl;

import java.util.Collection;
import java.util.Optional;

import at.shiftcontrol.lib.entity.Activity;
import at.shiftcontrol.shiftservice.dao.ActivityDao;
import at.shiftcontrol.shiftservice.dao.impl.specification.ActivitySpecifications;
import at.shiftcontrol.shiftservice.dto.event.schedule.ActivityScheduleDaySearchDto;
import at.shiftcontrol.shiftservice.repo.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ActivityDaoImpl implements ActivityDao {
    private final ActivityRepository activityRepository;

    @Override
    public @NonNull String getName() {
        return "Activity";
    }

    @Override
    public @NonNull Optional<Activity> findById(Long id) {
        return activityRepository.findById(id);
    }


    @Override
    public Activity save(Activity entity) {
        return activityRepository.save(entity);
    }

    @Override
    public Collection<Activity> saveAll(Collection<Activity> entities) {
        return activityRepository.saveAll(entities);
    }

    @Override
    public void delete(Activity entity) {
        activityRepository.delete(entity);
    }

    @Override
    public Collection<Activity> findAllByLocationId(Long locationId) {
        return activityRepository.findAllByLocationId(locationId);
    }

    @Override
    public Collection<Activity> findAllByEventId(Long eventId) {
        return activityRepository.findAllByEventId(eventId);
    }

    @Override
    public Collection<Activity> findAllWithoutLocationByEventId(Long eventId) {
        return activityRepository.findAllWithoutLocationByEventId(eventId);
    }

    @Override
    public Collection<Activity> searchActivitiesInEvent(Long eventId, ActivityScheduleDaySearchDto searchDto) {
        Specification<Activity> spec = ActivitySpecifications.matchesSearchDto(searchDto);

        return activityRepository.findAll(spec);
    }

    @Override
    public Optional<Activity> findByEventAndName(Long eventId, String name) {
        return activityRepository.findByEventAndName(eventId, name);
    }
}
