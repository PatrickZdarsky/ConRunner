package at.control.shiftcontrol.shift.dao;

import java.util.Collection;
import java.util.List;

import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.dto.event.schedule.EventScheduleFilterDto;

public interface ShiftDao extends BasicDao<Shift, Long> {
    List<Shift> searchUserRelatedShiftsInShiftPlan(long shiftPlanId, String userId);

    List<Shift> searchUserRelatedShiftsInEvent(long eventId, String userId);

    List<Shift> searchShiftsInEvent(long eventId, String userId, EventScheduleFilterDto filterDto);

    Collection<Shift> findAllInShiftPlan(Long shiftPlanId);

    List<Long> findAllShiftIdsInShiftPlan(Long shiftPlanId);
}
