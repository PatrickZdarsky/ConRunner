package at.conrunner.shift.repo;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.conrunner.shift.entity.TimeConstraint;
import at.conrunner.shift.lib.type.TimeConstraintType;

@Repository
public interface TimeConstraintRepository extends JpaRepository<TimeConstraint, Long> {
    @Query("""
        SELECT tc
        FROM TimeConstraint tc
        WHERE tc.volunteer.id = :volunteerId
          AND tc.event.id = :eventId
        """)
    Collection<TimeConstraint> searchByVolunteerAndEvent(java.util.UUID volunteerId, long eventId);

    @Query("""
        SELECT tc
        FROM TimeConstraint tc
        WHERE tc.volunteer.id = :volunteerId
          AND tc.event.id = :eventId
          AND tc.type = :type
        """)
    Collection<TimeConstraint> searchByVolunteerAndEventAndType(java.util.UUID volunteerId, long eventId, TimeConstraintType type);

    // Find the assignment and from there through the Positionslot the Shift.
    // Use the startTime and endTime from the Shift to find a TimeConstraint of the given volunteer in the same time range
    // Also check that the event is the same
    @Query("""
        SELECT tc
        FROM TimeConstraint tc
        JOIN Assignment a ON tc.volunteer = a.assignedVolunteer
        JOIN PositionSlot ps ON a.positionSlot = ps
        JOIN Shift s ON ps.shift = s
        WHERE a.id = :assignmentId
          AND tc.event = s.shiftPlan.event
          AND tc.startTime <= s.endTime AND tc.endTime >= s.startTime
          AND tc.type = :type
        """)
    Optional<TimeConstraint> findByAssignmentIdAndType(long assignmentId, TimeConstraintType type);

    @Query("""
        SELECT tc
        FROM TimeConstraint tc
        WHERE tc.volunteer.id = :volunteerId
          AND EXISTS (
              SELECT 1
              FROM PositionSlot ps
              JOIN ps.shift s
              WHERE ps.id = :positionSlotId
                AND tc.event = s.shiftPlan.event
                AND tc.startTime <= s.endTime AND tc.endTime >= s.startTime
          )
        """)
    Collection<TimeConstraint> findByPositionSlotIdAndVolunteerId(long positionSlotId, java.util.UUID volunteerId);
}
