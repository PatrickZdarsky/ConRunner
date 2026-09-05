package at.control.shiftcontrol.shift.positionslot;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionSlotRepository extends JpaRepository<PositionSlotEntity, UUID> {
    boolean existsByShiftIdAndNameIgnoreCase(UUID shiftId, String name);

    Collection<PositionSlotEntity> findAllByShiftIdOrderByNameAsc(UUID shiftId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from PositionSlotEntity slot where slot.id = :positionSlotId")
    Optional<PositionSlotEntity> findByIdForUpdate(@Param("positionSlotId") UUID positionSlotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from PositionSlotEntity slot where slot.id in :positionSlotIds order by slot.id")
    List<PositionSlotEntity> findAllByIdInForUpdate(@Param("positionSlotIds") Collection<UUID> positionSlotIds);
}
