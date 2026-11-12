package at.conrunner.shift.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import at.conrunner.shift.entity.PositionConstraint;
import at.conrunner.shift.entity.PositionConstraintId;

@Repository
public interface PositionConstraintRepository extends JpaRepository<PositionConstraint, PositionConstraintId> {
}
