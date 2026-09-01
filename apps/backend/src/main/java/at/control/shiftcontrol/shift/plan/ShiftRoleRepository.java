package at.control.shiftcontrol.shift.plan;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRoleRepository extends JpaRepository<ShiftRoleEntity, UUID> {
    boolean existsByPlanIdAndNameIgnoreCase(UUID planId, String name);

    Optional<ShiftRoleEntity> findByPlanIdAndNameIgnoreCase(UUID planId, String name);

    Collection<ShiftRoleEntity> findAllByPlanIdOrderByNameAsc(UUID planId);
}
