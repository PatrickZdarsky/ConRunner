package at.control.shiftcontrol.shift.dao.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.shiftcontrol.lib.entity.Shift;
import at.shiftcontrol.shiftservice.dao.ShiftDao;
import at.shiftcontrol.shiftservice.dao.impl.specification.ShiftSpecifications;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleFilterDto;
import at.shiftcontrol.shiftservice.repo.ShiftRepository;

@RequiredArgsConstructor
@Component
public class ShiftDaoImpl implements ShiftDao {
    private final ShiftRepository shiftRepository;

    @Override
    public @NonNull String getName() {
        return "Shift";
    }

    @Override
    public @NonNull Optional<Shift> findById(Long id) {
        return shiftRepository.findById(id);
    }

    @Override
    public Shift save(Shift entity) {
        return shiftRepository.save(entity);
    }

    @Override
    public Collection<Shift> saveAll(Collection<Shift> entities) {
        return shiftRepository.saveAll(entities);
    }

    @Override
    public void delete(Shift entity) {
        shiftRepository.delete(entity);
    }


    @Override
    public List<Shift> searchUserRelatedShiftsInShiftPlan(long shiftPlanId, String userId) {
        var spec = ShiftSpecifications.inShiftPlan(shiftPlanId)
            .and(ShiftSpecifications.assignedToUser(userId));

        return shiftRepository.findAll(spec);
    }

    @Override
    public Collection<Shift> findAllInShiftPlan(Long shiftPlanId) {
        var spec = ShiftSpecifications.inShiftPlan(shiftPlanId);

        return shiftRepository.findAll(spec);
    }

    @Override
    public List<Long> findAllShiftIdsInShiftPlan(Long shiftPlanId) {
        return shiftRepository.findAllShiftIdsInShiftPlan(shiftPlanId);
    }

    @Override
    public List<Shift> searchUserRelatedShiftsInEvent(long eventId, String userId) {
        var spec = ShiftSpecifications.inEvent(eventId)
            .and(ShiftSpecifications.assignedToUser(userId));

        return shiftRepository.findAll(spec);
    }

    @Override
    public List<Shift> searchShiftsInEvent(long eventId, String userId, EventScheduleFilterDto filterDto) {
        Specification<Shift> spec = ShiftSpecifications.inEvent(eventId)
            .and(ShiftSpecifications.matchesSearchDto(filterDto)); // your other filters

        return shiftRepository.findAll(spec);
    }
}
