package at.conrunner.shift.dao.impl;

import java.util.UUID;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.conrunner.shift.dto.rows.PlanVolunteerIdRow;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.platform.exception.PartiallyNotFoundException;
import at.conrunner.shift.dao.ShiftPlanDao;
import at.conrunner.shift.repo.PositionConstraintRepository;
import at.conrunner.shift.repo.PositionSlotPreferenceRepository;
import at.conrunner.shift.repo.ShiftPlanRepository;
import at.conrunner.shift.repo.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ShiftPlanDaoImpl implements ShiftPlanDao {
    private final ShiftPlanRepository shiftPlanRepository;
    private final VolunteerRepository volunteerRepository;
    private final PositionConstraintRepository positionConstraintRepository;
    private final PositionSlotPreferenceRepository positionSlotPreferenceRepository;

    @Override
    public @NonNull String getName() {
        return "ShiftPlan";
    }

    @Override
    public @NonNull Optional<ShiftPlan> findById(Long id) {
        return shiftPlanRepository.findById(id);
    }

    @Override
    public ShiftPlan save(ShiftPlan entity) {
        return shiftPlanRepository.save(entity);
    }

    @Override
    public Collection<ShiftPlan> saveAll(Collection<ShiftPlan> entities) {
        return shiftPlanRepository.saveAll(entities);
    }

    @Override
    public void delete(ShiftPlan entity) {
        var volunteers = volunteerRepository.findAllReferencingPlan(entity);
        for (var volunteer : volunteers) {
            volunteer.getVolunteeringPlans().remove(entity);
            volunteer.getPlanningPlans().remove(entity);
            volunteer.getLockedPlans().remove(entity);
            volunteer.getRoles().removeIf(role -> role.getShiftPlan().getId() == entity.getId());
        }
        positionConstraintRepository.deleteForPlan(entity.getId());
        positionSlotPreferenceRepository.deleteForPlan(entity.getId());
        shiftPlanRepository.delete(entity);
    }

    @Override
    public Collection<ShiftPlan> findByEventId(Long eventId) {
        return shiftPlanRepository.findByEventId(eventId);
    }

    @Override
    public boolean existsInEvent(long eventId, Set<Long> planIds) {
        return !planIds.isEmpty() && shiftPlanRepository.existsByEventIdAndIdIn(eventId, planIds);
    }

    @Override
    public Set<Long> findEventIdsForUser(UUID userId) {
        return shiftPlanRepository.findEventIdsForUser(userId);
    }

    @Override
    public Collection<PlanVolunteerIdRow> getPlannersForEventAndUser(long eventId, UUID userId) {
        return shiftPlanRepository.getPlannersForEventAndUser(eventId, userId);
    }

    @Override
    public Collection<ShiftPlan> findAll() {
        return shiftPlanRepository.findAll();
    }

    @Override
    public Collection<ShiftPlan> findAllUserRelatedShiftPlansInEvent(UUID userId, String eventId) {
        return shiftPlanRepository.findAllUserRelatedShiftPlansInEvent(userId, eventId);
    }

    @Override
    public Collection<ShiftPlan> getByIds(Set<Long> shiftPlanIds) {
        var plans = shiftPlanRepository.getByIds(shiftPlanIds);
        if (plans.size() != shiftPlanIds.size()) {
            var foundId = plans.stream()
                .map(ShiftPlan::getId)
                .collect(Collectors.toSet());
            shiftPlanIds.removeAll(foundId);
            throw PartiallyNotFoundException.of(getName(), shiftPlanIds);
        }
        return plans;
    }
}
