package at.control.shiftcontrol.shift.plan;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftPlanRepository extends JpaRepository<ShiftPlanEntity, UUID> {
    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    Collection<ShiftPlanEntity> findAllByEventIdOrderByNameAsc(UUID eventId);
}
