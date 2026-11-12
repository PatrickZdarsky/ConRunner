package at.conrunner.shift.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.conrunner.shift.entity.Shift;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long>, JpaSpecificationExecutor<Shift> {
    @Query("SELECT s.id FROM Shift s WHERE s.shiftPlan.id = :shiftPlanId")
    List<Long> findAllShiftIdsInShiftPlan(Long shiftPlanId);
}
