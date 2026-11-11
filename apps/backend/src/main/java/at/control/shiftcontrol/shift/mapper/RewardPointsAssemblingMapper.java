package at.control.shiftcontrol.shift.mapper;

import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsDto;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsCalculator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RewardPointsAssemblingMapper {
    private final RewardPointsCalculator rewardPointsCalculator;

    public RewardPointsDto toDto(@NonNull PositionSlot positionSlot) {
        var currentRewardPoints = rewardPointsCalculator.calculateForAssignment(positionSlot).rewardPoints();
        var rewardPointsConfigHash = rewardPointsCalculator.calculatePointsConfigHash(positionSlot);

        return RewardPointsDto.builder()
            .currentRewardPoints(currentRewardPoints)
            .overrideRewardPoints(positionSlot.getOverrideRewardPoints())
            .rewardPointsConfigHash(rewardPointsConfigHash)
            .build();
    }
}
