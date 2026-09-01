package at.control.shiftcontrol.shift.positionslot;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffingShiftRepository extends JpaRepository<StaffingShiftEntity, UUID> {
    Collection<StaffingShiftEntity> findAllByPlanIdOrderByStartTimeAsc(UUID planId);
}
