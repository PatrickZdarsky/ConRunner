package at.conrunner.shift.service.rewardpoints;

import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.dto.rewardpoints.RewardPointsSnapshotDto;

public interface RewardPointsCalculator {
    RewardPointsSnapshotDto calculateForAssignment(PositionSlot slot);

    String calculatePointsConfigHash(PositionSlot slot);
}
