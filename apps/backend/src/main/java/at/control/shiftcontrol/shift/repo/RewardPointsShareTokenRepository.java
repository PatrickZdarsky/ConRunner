package at.control.shiftcontrol.shift.repo;

import at.control.shiftcontrol.shift.entity.RewardPointsShareToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardPointsShareTokenRepository extends JpaRepository<RewardPointsShareToken, Long> {
    boolean existsByToken(String token);

    boolean existsByNameIgnoreCase(String name);
}
