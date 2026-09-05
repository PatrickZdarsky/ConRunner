package at.control.shiftcontrol.shift;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<ShiftEntity, UUID> {
    Collection<ShiftEntity> findAllByPlanIdOrderByStartTimeAsc(UUID planId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shift from ShiftEntity shift where shift.id = :shiftId")
    Optional<ShiftEntity> findByIdForUpdate(@Param("shiftId") UUID shiftId);
}
