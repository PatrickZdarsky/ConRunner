package at.conrunner.shift.dto.rewardpoints;

import at.conrunner.shift.entity.RewardPointsTransaction;

public record BookingResultDto(
    boolean created,
    RewardPointsTransaction transaction
) {
}
