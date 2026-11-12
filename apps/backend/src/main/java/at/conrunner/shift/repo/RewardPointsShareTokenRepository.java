package at.conrunner.shift.repo;

import at.conrunner.shift.entity.RewardPointsShareToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardPointsShareTokenRepository extends JpaRepository<RewardPointsShareToken, Long> {
    boolean existsByToken(String token);

    boolean existsByNameIgnoreCase(String name);
}
