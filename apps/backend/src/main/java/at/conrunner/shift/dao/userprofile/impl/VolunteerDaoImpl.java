package at.conrunner.shift.dao.userprofile.impl;

import java.util.Collection;
import java.util.UUID;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.repo.VolunteerRepository;

@RequiredArgsConstructor
@Component
public class VolunteerDaoImpl implements VolunteerDao {
    private final VolunteerRepository volunteerRepository;

    @Override
    public @NonNull String getName() {
        return "Volunteer";
    }

    @Override
    public @NonNull Optional<Volunteer> findById(UUID id) {
        return volunteerRepository.findById(id);
    }

    @Override
    public Collection<Volunteer> findAllByShiftPlan(long shiftPlanId) {
        return volunteerRepository.findAllByShiftPlan(shiftPlanId);
    }

    @Override
    public Collection<UUID> findAllIdsByShiftPlan(long shiftPlanId) {
        return volunteerRepository.findAllIdsByShiftPlan(shiftPlanId);
    }

    @Override
    public Collection<Volunteer> findAllPlannersByShiftPlan(long id) {
        return volunteerRepository.findAllPlannersByShiftPlan(id);
    }

    @Override
    public Collection<Volunteer> findAllByEvent(long eventId) {
        return volunteerRepository.findAllByEvent(eventId);
    }

    @Override
    public Collection<Volunteer> findAllPlannersByEvent(long eventId) {
        return volunteerRepository.findAllPlannersByEvent(eventId);
    }

    @Override
    public Collection<Volunteer> findAllByVolunteerIds(Collection<UUID> volunteerIds) {
        return volunteerRepository.findAllByVolunteerIds(volunteerIds);
    }

    @Override
    public Collection<Volunteer> findAllByPlannerIds(Collection<UUID> plannerIds) {
        return volunteerRepository.findAllByPlannerIds(plannerIds);
    }

    @Override
    public Volunteer save(Volunteer entity) {
        return volunteerRepository.save(entity);
    }

    @Override
    public Collection<Volunteer> saveAll(Collection<Volunteer> entities) {
        return volunteerRepository.saveAll(entities);
    }

    @Override
    public void delete(Volunteer entity) {
        volunteerRepository.delete(entity);
    }

    @Override
    public Collection<Volunteer> findAllByShiftPlanAndVolunteerIds(long shiftPlanId, Collection<UUID> volunteerIds) {
        return volunteerRepository.findAllByShiftPlanAndVolunteerIds(shiftPlanId, volunteerIds);
    }

    @Override
    public Collection<Volunteer> findAllByEventAndVolunteerIds(long eventId, Collection<UUID> volunteerIds) {
        return volunteerRepository.findAllByEventAndVolunteerIds(eventId, volunteerIds);
    }

    @Override
    public Collection<Volunteer> findAllByShiftPlanAndPlannerIds(long shiftPlanId, Collection<UUID> plannerIds) {
        return volunteerRepository.findAllByShiftPlanAndPlannerIds(shiftPlanId, plannerIds);
    }

    @Override
    public Collection<Volunteer> findAllByEventAndPlannerIds(long eventId, Collection<UUID> plannerIds) {
        return volunteerRepository.findAllByEventAndPlannerIds(eventId, plannerIds);
    }

    @Override
    public Collection<Volunteer> findAllPaginated(long page, long size) {
        return volunteerRepository.findAllPaginated(page * size, size);
    }

    @Override
    public Collection<Volunteer> findAllByShiftPlanPaginated(long page, long size, long shiftPlanId) {
        return volunteerRepository.findAllByShiftPlanPaginated(page * size, size, shiftPlanId);
    }

    @Override
    public long findAllSize() {
        return volunteerRepository.findAllSize();
    }

    @Override
    public long findAllByShiftPlanSize(long shiftPlanId) {
        return volunteerRepository.findAllByShiftPlanSize(shiftPlanId);
    }

    @Override
    public Optional<Volunteer> findByCalendarShareId(String calendarShareId) {
        return volunteerRepository.findByCalendarShareId(calendarShareId);
    }

    @Override
    public boolean existsByCalendarShareId(String calendarShareId) {
        return volunteerRepository.existsByCalendarShareId(calendarShareId);
    }

    @Override
    public boolean existsById(UUID uuid) {
        return volunteerRepository.existsById(uuid);
    }
}
