package at.control.shiftcontrol.shift.positionslot;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionSlotRepository extends JpaRepository<PositionSlotEntity, UUID> {
    boolean existsByStaffingShiftIdAndNameIgnoreCase(UUID staffingShiftId, String name);

    Collection<PositionSlotEntity> findAllByStaffingShiftIdOrderByNameAsc(UUID staffingShiftId);
}
