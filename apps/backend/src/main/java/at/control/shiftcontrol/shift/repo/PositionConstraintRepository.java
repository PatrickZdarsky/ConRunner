package at.control.shiftcontrol.shift.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import at.shiftcontrol.lib.entity.PositionConstraint;
import at.shiftcontrol.lib.entity.PositionConstraintId;

@Repository
public interface PositionConstraintRepository extends JpaRepository<PositionConstraint, PositionConstraintId> {
}
