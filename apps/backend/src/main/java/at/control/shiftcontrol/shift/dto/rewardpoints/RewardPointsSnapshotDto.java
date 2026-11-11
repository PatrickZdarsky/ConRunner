package at.control.shiftcontrol.shift.dto.rewardpoints;

import java.util.Map;

public record RewardPointsSnapshotDto(
    int rewardPoints,
    Map<String, Object> metadata
) {
}
