package at.control.shiftcontrol.shift.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import at.control.shiftcontrol.shift.entity.PositionConstraint;
import at.control.shiftcontrol.shift.entity.PositionConstraintId;

@Repository
public interface PositionConstraintRepository extends JpaRepository<PositionConstraint, PositionConstraintId> {
}
