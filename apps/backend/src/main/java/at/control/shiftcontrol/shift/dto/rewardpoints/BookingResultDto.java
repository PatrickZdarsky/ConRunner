package at.control.shiftcontrol.shift.dto.rewardpoints;

import at.control.shiftcontrol.shift.entity.RewardPointsTransaction;

public record BookingResultDto(
    boolean created,
    RewardPointsTransaction transaction
) {
}
