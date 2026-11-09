package at.control.shiftcontrol.shift.repo.role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.shiftcontrol.lib.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    @Query("""
        SELECT DISTINCT r
        FROM Role r
        WHERE r.shiftPlan.id = :shiftPlanId
        """)
    List<Role> findAllByShiftPlanId(Long shiftPlanId);

    @Query("""
        SELECT DISTINCT r
        FROM Role r
        WHERE r.id in :roleIds
                AND r.shiftPlan.id = :shiftPlanId
        """)
    Collection<Role> getRolesByIdsAndShiftPlanId(Set<Long> roleIds, Long shiftPlanId);

    @Query("""
        SELECT DISTINCT r
        FROM Role r
        WHERE r.id in :roleIds
        """)
    Collection<Role> getByIds(Set<Long> roleIds);

    @Query("""
        SELECT r
        FROM Role r
        WHERE r.name = :name
          AND r.shiftPlan.id = :shiftPlanId
        """)
    Optional<Role> findByNameAndShiftPlanId(String name, long shiftPlanId);
}
