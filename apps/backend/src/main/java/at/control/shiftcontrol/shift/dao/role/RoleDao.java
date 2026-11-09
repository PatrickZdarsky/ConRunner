package at.control.shiftcontrol.shift.dao.role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import at.shiftcontrol.lib.entity.Role;
import at.shiftcontrol.shiftservice.dao.BasicDao;

public interface RoleDao extends BasicDao<Role, Long> {
    List<Role> findAllByShiftPlanId(Long eventId);

    Collection<Role> findAllById(Collection<Long> roleIds);

    Collection<Role> getRolesByIdsAndShiftPlanId(Set<Long> roleIds, Long shiftPlanId);

    Collection<Role> getByIds(Set<Long> roleIds);

    Optional<Role> findByNameAndShiftPlanId(String name, long shiftPlanId);
}
