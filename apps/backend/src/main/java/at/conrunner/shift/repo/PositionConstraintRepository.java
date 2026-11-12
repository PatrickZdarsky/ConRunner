package at.conrunner.shift.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.conrunner.shift.entity.PositionConstraint;
import at.conrunner.shift.entity.PositionConstraintId;

@Repository
public interface PositionConstraintRepository extends JpaRepository<PositionConstraint, PositionConstraintId> {
    @Modifying
    @Query("""
        DELETE FROM PositionConstraint c
        WHERE c.id.sourcePositionSlotId IN (SELECT s.id FROM PositionSlot s WHERE s.shift.shiftPlan.id = :planId)
           OR c.id.targetPositionSlotId IN (SELECT s.id FROM PositionSlot s WHERE s.shift.shiftPlan.id = :planId)
        """)
    void deleteForPlan(long planId);
}
