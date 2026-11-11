package at.control.shiftcontrol.shift.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import at.control.shiftcontrol.shift.entity.PositionSlotPreference;
import at.control.shiftcontrol.shift.entity.PositionSlotPreferenceId;

@Repository
public interface PositionSlotPreferenceRepository extends JpaRepository<PositionSlotPreference, PositionSlotPreferenceId> {
}
