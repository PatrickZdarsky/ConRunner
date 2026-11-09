package at.control.shiftcontrol.shift.dao;

import java.util.Collection;
import java.util.Optional;

import at.control.shiftcontrol.shift.entity.TimeConstraint;
import at.control.shiftcontrol.shift.lib.type.TimeConstraintType;

public interface TimeConstraintDao extends BasicDao<TimeConstraint, Long> {
    Collection<TimeConstraint> searchByVolunteerAndEvent(String volunteerId, long eventId);

    Collection<TimeConstraint> searchByVolunteerAndEventAndType(String volunteerId, long eventId, TimeConstraintType type);

    Optional<TimeConstraint> findByAssignmentIdAndType(long assignmentId, TimeConstraintType type);

    Collection<TimeConstraint> findByPositionSlotIdAndVolunteerId(long positionSlotId, String volunteerId);
}
