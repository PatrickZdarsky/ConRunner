package at.conrunner.shift.dao;

import java.util.Collection;

import at.conrunner.shift.entity.RewardPointsShareToken;


public interface RewardPointsShareTokenDao extends BasicDao<RewardPointsShareToken, Long> {
    Collection<RewardPointsShareToken> findAll();

    boolean existsByToken(String token);

    boolean existsByNameIgnoreCase(String name);
}
