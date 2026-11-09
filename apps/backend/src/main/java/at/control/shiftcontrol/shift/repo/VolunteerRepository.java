package at.control.shiftcontrol.shift.repo;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import at.shiftcontrol.lib.entity.Volunteer;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, String> {
    Optional<Volunteer> findById(String userId);

    Optional<Volunteer> findByCalendarShareId(String calendarShareId);

    boolean existsByCalendarShareId(String calendarShareId);

    @Query("""
            SELECT v
            FROM Volunteer v
            ORDER BY v.id DESC
            LIMIT :size OFFSET :offset
        """)
    Collection<Volunteer> findAllPaginated(long offset, long size);

    @Query("""
            SELECT count(v)
            FROM Volunteer v
        """)
    long findAllSize();

    @Query("""
            SELECT count(v)
            FROM Volunteer v
            JOIN v.volunteeringPlans p
            WHERE p.id = :shiftPlanId
        """)
    long findAllByShiftPlanSize(long shiftPlanId);

    @Query("""
            select (count(sp) > 0)
            from Volunteer v
            join v.volunteeringPlans sp
            where v.id = :volunteerId and sp.id = :shiftPlanId
        """)
    boolean isVolunteerInShiftPlan(@Param("volunteerId") String volunteerId,
                                   @Param("shiftPlanId") long shiftPlanId);

    @Query("""
            select (count(sp) > 0)
            from Volunteer v
            join v.planningPlans sp
            where v.id = :volunteerId and sp.id = :shiftPlanId
        """)
    boolean isPlannerInShiftPlan(@Param("volunteerId") String volunteerId,
                                 @Param("shiftPlanId") long shiftPlanId);

    @Query("""
            select (count(r) > 0)
            from Volunteer v
            join v.roles r
            where v.id = :volunteerId and r.id = :roleId
        """)
    boolean hasUserRole(@Param("volunteerId") String volunteerId,
                        @Param("roleId") long roleId);

    @Query("""
            SELECT v
            FROM Volunteer v
            JOIN v.volunteeringPlans p
            WHERE p.id = :shiftPlanId
        """)
    Collection<Volunteer> findAllByShiftPlan(long shiftPlanId);

    @Query("""
            SELECT v.id
            FROM Volunteer v
            JOIN v.volunteeringPlans p
            WHERE p.id = :shiftPlanId
        """)
    Collection<String> findAllIdsByShiftPlan(long shiftPlanId);

    @Query("""
            SELECT v
            FROM Volunteer v
            JOIN v.volunteeringPlans p
            WHERE p.id = :shiftPlanId
            ORDER BY v.id DESC
            LIMIT :size OFFSET :offset
        """)
    Collection<Volunteer> findAllByShiftPlanPaginated(long offset, long size, long shiftPlanId);

    @Query("""
            SELECT v
            FROM Volunteer v
            JOIN v.volunteeringPlans p
            JOIN p.event e
            WHERE e.id = :eventId
        """)
    Collection<Volunteer> findAllByEvent(long eventId);

    @Query("""
            SELECT v
            FROM Volunteer v
            JOIN v.planningPlans p
            WHERE p.id = :shiftPlanId
        """)
    Collection<Volunteer> findAllPlannersByShiftPlan(long shiftPlanId);

    @Query("""
            SELECT DISTINCT v
            FROM Volunteer v
            JOIN v.planningPlans p
            WHERE p.id = :shiftPlanId
              AND v.id IN :plannerIds
        """)
    Collection<Volunteer> findAllByShiftPlanAndPlannerIds(long shiftPlanId, Collection<String> plannerIds);

    @Query("""
            SELECT DISTINCT v
            FROM Volunteer v
            JOIN v.planningPlans p
            WHERE p.event.id = :eventId
              AND v.id IN :plannerIds
        """)
    Collection<Volunteer> findAllByEventAndPlannerIds(long eventId, Collection<String> plannerIds);

    @Query("""
            SELECT DISTINCT v
            FROM Volunteer v
            JOIN v.volunteeringPlans p
            WHERE p.id = :shiftPlanId
              AND v.id IN :volunteerIds
        """)
    Collection<Volunteer> findAllByShiftPlanAndVolunteerIds(long shiftPlanId, Collection<String> volunteerIds);

    @Query("""
            SELECT DISTINCT v
            FROM Volunteer v
            JOIN v.volunteeringPlans p
            WHERE p.event.id = :eventId
              AND v.id IN :volunteerIds
        """)
    Collection<Volunteer> findAllByEventAndVolunteerIds(long eventId, Collection<String> volunteerIds);

    @Query("""
            SELECT DISTINCT v
            FROM Volunteer v
            JOIN v.planningPlans p
            WHERE p.event.id = :eventId
        """)
    Collection<Volunteer> findAllPlannersByEvent(long eventId);

    @Query("""
            SELECT DISTINCT v
            FROM Volunteer v
            WHERE v.id IN :volunteerIds
        """)
    Collection<Volunteer> findAllByVolunteerIds(Collection<String> volunteerIds);

    @Query("""
            select (count(sp) > 0)
            from Volunteer v
            join v.planningPlans sp
            where v.id IN :plannerIds
        """)
    Collection<Volunteer> findAllByPlannerIds(Collection<String> plannerIds);
}
