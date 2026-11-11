package at.control.shiftcontrol.shift.service.rewardpoints;

import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsSnapshotDto;

public interface RewardPointsCalculator {
    RewardPointsSnapshotDto calculateForAssignment(PositionSlot slot);

    String calculatePointsConfigHash(PositionSlot slot);
}
