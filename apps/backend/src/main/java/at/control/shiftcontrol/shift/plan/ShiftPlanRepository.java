package at.control.shiftcontrol.shift.plan;

import java.util.Collection;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftPlanRepository extends JpaRepository<ShiftPlanEntity, UUID> {
    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    Collection<ShiftPlanEntity> findAllByEventIdOrderByNameAsc(UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from ShiftPlanEntity plan where plan.id = :planId")
    java.util.Optional<ShiftPlanEntity> findByIdForUpdate(@Param("planId") UUID planId);
}
