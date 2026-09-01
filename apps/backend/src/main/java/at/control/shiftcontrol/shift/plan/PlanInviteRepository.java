package at.control.shiftcontrol.shift.plan;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PlanInviteRepository extends JpaRepository<PlanInviteEntity, UUID> {
    Collection<PlanInviteEntity> findAllByPlanIdOrderByCreatedAtDesc(UUID planId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invite from PlanInviteEntity invite where invite.code = :code")
    Optional<PlanInviteEntity> findByCodeForUpdate(String code);
}
