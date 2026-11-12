package at.conrunner.shift.dao;

import java.util.UUID;
import java.util.Collection;
import java.util.Optional;

import at.conrunner.shift.entity.TimeConstraint;
import at.conrunner.shift.lib.type.TimeConstraintType;

public interface TimeConstraintDao extends BasicDao<TimeConstraint, Long> {
    Collection<TimeConstraint> findAllByEventId(long eventId);

    Collection<TimeConstraint> searchByVolunteerAndEvent(UUID volunteerId, long eventId);

    Collection<TimeConstraint> searchByVolunteerAndEventAndType(UUID volunteerId, long eventId, TimeConstraintType type);

    Optional<TimeConstraint> findByAssignmentIdAndType(long assignmentId, TimeConstraintType type);

    Collection<TimeConstraint> findByPositionSlotIdAndVolunteerId(long positionSlotId, UUID volunteerId);
}
