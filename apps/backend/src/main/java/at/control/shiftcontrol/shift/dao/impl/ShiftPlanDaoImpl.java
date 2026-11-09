package at.control.shiftcontrol.shift.dao.impl;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.exception.PartiallyNotFoundException;
import at.shiftcontrol.shiftservice.dao.ShiftPlanDao;
import at.shiftcontrol.shiftservice.repo.ShiftPlanRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ShiftPlanDaoImpl implements ShiftPlanDao {
    private final ShiftPlanRepository shiftPlanRepository;

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
        shiftPlanRepository.delete(entity);
    }

    @Override
    public Collection<ShiftPlan> findByEventId(Long eventId) {
        return shiftPlanRepository.findByEventId(eventId);
    }

    @Override
    public Collection<ShiftPlan> findAll() {
        return shiftPlanRepository.findAll();
    }

    @Override
    public Collection<ShiftPlan> findAllUserRelatedShiftPlansInEvent(String userId, String eventId) {
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
