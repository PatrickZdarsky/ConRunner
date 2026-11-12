package at.conrunner.shift.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.conrunner.shift.entity.PositionSlotPreference;
import at.conrunner.shift.entity.PositionSlotPreferenceId;

@Repository
public interface PositionSlotPreferenceRepository extends JpaRepository<PositionSlotPreference, PositionSlotPreferenceId> {
    @Modifying
    @Query("""
        DELETE FROM PositionSlotPreference p
        WHERE p.id.positionSlotId IN (SELECT s.id FROM PositionSlot s WHERE s.shift.shiftPlan.id = :planId)
        """)
    void deleteForPlan(long planId);
}
