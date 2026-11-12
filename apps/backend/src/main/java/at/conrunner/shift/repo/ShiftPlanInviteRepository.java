package at.conrunner.shift.repo;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import at.conrunner.shift.entity.ShiftPlanInvite;

public interface ShiftPlanInviteRepository extends JpaRepository<ShiftPlanInvite, Long> {
    Optional<ShiftPlanInvite> findByCode(String code);

    Collection<ShiftPlanInvite> findAllByShiftPlanId(long shiftPlanId);

    boolean existsByCode(String code);
}
