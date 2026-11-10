package at.control.shiftcontrol.shift.dao;

import java.util.UUID;
import java.util.Collection;
import java.util.Set;

import at.control.shiftcontrol.shift.entity.ShiftPlan;

public interface ShiftPlanDao extends BasicDao<ShiftPlan, Long> {
    Collection<ShiftPlan> findByEventId(Long eventId);

    Collection<ShiftPlan> findAll();

    Collection<ShiftPlan> findAllUserRelatedShiftPlansInEvent(UUID userId, String eventId);

    Collection<ShiftPlan> getByIds(Set<Long> shiftPlanIds);
}
