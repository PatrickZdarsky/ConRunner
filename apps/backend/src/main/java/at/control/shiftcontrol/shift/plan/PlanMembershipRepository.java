package at.control.shiftcontrol.shift.plan;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanMembershipRepository extends JpaRepository<PlanMembershipEntity, UUID> {
    Optional<PlanMembershipEntity> findByPlanIdAndUserId(UUID planId, UUID userId);

    boolean existsByPlanIdAndUserId(UUID planId, UUID userId);

    boolean existsByPlanIdAndUserIdAndPlannerTrue(UUID planId, UUID userId);

    Collection<PlanMembershipEntity> findAllByPlanId(UUID planId);

    Collection<PlanMembershipEntity> findAllByUserId(UUID userId);
}
