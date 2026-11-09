package at.control.shiftcontrol.shift.service.rewardpoints;

import at.shiftcontrol.lib.entity.PositionSlot;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsSnapshotDto;

public interface RewardPointsCalculator {
    RewardPointsSnapshotDto calculateForAssignment(PositionSlot slot);

    String calculatePointsConfigHash(PositionSlot slot);
}
