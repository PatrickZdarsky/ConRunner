package at.conrunner.shift.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import at.conrunner.shift.entity.PositionSlotPreference;
import at.conrunner.shift.entity.PositionSlotPreferenceId;

@Repository
public interface PositionSlotPreferenceRepository extends JpaRepository<PositionSlotPreference, PositionSlotPreferenceId> {
}
