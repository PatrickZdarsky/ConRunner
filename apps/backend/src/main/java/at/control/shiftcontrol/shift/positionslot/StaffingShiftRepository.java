package at.control.shiftcontrol.shift.positionslot;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffingShiftRepository extends JpaRepository<StaffingShiftEntity, UUID> {
    Collection<StaffingShiftEntity> findAllByPlanIdOrderByStartTimeAsc(UUID planId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select staffingShift from StaffingShiftEntity staffingShift where staffingShift.id = :staffingShiftId")
    Optional<StaffingShiftEntity> findByIdForUpdate(@Param("staffingShiftId") UUID staffingShiftId);
}
