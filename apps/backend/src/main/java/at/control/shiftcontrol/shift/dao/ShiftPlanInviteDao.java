package at.control.shiftcontrol.shift.dao;

import java.util.Collection;

import at.control.shiftcontrol.shift.entity.ShiftPlanInvite;

public interface ShiftPlanInviteDao extends BasicDao<ShiftPlanInvite, Long> {
    ShiftPlanInvite getByCode(String code);

    boolean existsByCode(String code);

    Collection<ShiftPlanInvite> findAllByShiftPlanId(Long shiftPlanId);
}
