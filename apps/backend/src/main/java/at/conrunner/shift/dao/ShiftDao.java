package at.conrunner.shift.dao;

import java.util.UUID;
import java.util.Collection;
import java.util.List;

import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.dto.event.schedule.EventScheduleFilterDto;

public interface ShiftDao extends BasicDao<Shift, Long> {
    List<Shift> searchUserRelatedShiftsInShiftPlan(long shiftPlanId, UUID userId);

    List<Shift> searchUserRelatedShiftsInEvent(long eventId, UUID userId);

    List<Shift> searchShiftsInEvent(long eventId, UUID userId, EventScheduleFilterDto filterDto);

    Collection<Shift> findAllInShiftPlan(Long shiftPlanId);

    List<Long> findAllShiftIdsInShiftPlan(Long shiftPlanId);
}
