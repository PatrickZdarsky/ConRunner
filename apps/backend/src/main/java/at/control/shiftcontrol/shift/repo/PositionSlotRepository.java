package at.control.shiftcontrol.shift.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import at.control.shiftcontrol.shift.entity.PositionSlot;

@Repository
public interface PositionSlotRepository extends JpaRepository<PositionSlot, Long> {
}
