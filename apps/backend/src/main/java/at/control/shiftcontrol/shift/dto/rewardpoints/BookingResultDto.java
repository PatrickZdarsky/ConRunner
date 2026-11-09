package at.control.shiftcontrol.shift.dto.rewardpoints;

import at.shiftcontrol.lib.entity.RewardPointsTransaction;

public record BookingResultDto(
    boolean created,
    RewardPointsTransaction transaction
) {
}
