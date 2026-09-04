package at.control.shiftcontrol.shift.assignment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentTransitionRepository extends JpaRepository<AssignmentTransitionEntity, UUID> {
}
