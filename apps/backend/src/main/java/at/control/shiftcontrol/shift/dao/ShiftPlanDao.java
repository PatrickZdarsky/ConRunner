package at.control.shiftcontrol.shift.dao;

import java.util.Collection;
import java.util.Set;

import at.shiftcontrol.lib.entity.ShiftPlan;

public interface ShiftPlanDao extends BasicDao<ShiftPlan, Long> {
    Collection<ShiftPlan> findByEventId(Long eventId);

    Collection<ShiftPlan> findAll();

    Collection<ShiftPlan> findAllUserRelatedShiftPlansInEvent(String userId, String eventId);

    Collection<ShiftPlan> getByIds(Set<Long> shiftPlanIds);
}
