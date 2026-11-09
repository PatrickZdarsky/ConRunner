package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;
import java.util.Map;

import at.shiftcontrol.lib.entity.Assignment;
import at.shiftcontrol.lib.entity.RewardPointsShareToken;
import at.shiftcontrol.shiftservice.dto.rewardpoints.EventPointsDto;
import at.shiftcontrol.shiftservice.dto.rewardpoints.EventPointsInternalDto;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsShareTokenDto;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsTransactionDto;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RewardPointsMapper {
    public static EventPointsDto toEventPointsDto(EventPointsInternalDto eventPointsInternalDto) {
        return new EventPointsDto(String.valueOf(eventPointsInternalDto.eventId()), (int) eventPointsInternalDto.points());
    }

    public static Collection<EventPointsDto> toEventPointsDto(Collection<EventPointsInternalDto> eventPointsInternalDtos) {
        return eventPointsInternalDtos.stream()
            .map(RewardPointsMapper::toEventPointsDto)
            .toList();
    }

    public static RewardPointsTransactionDto toRewardPointsTransactionDto(@NonNull Assignment assignment, int points, String sourceKey,
                                                                          Map<String, Object> metadata) {
        return RewardPointsTransactionDto.builder()
            .userId(assignment.getAssignedVolunteer().getId())
            .eventId(assignment.getPositionSlot().getShift().getShiftPlan().getEvent().getId())
            .shiftPlanId(assignment.getPositionSlot().getShift().getShiftPlan().getId())
            .positionSlotId(assignment.getPositionSlot().getId())
            .pointsSnapshot(points)
            .sourceKey(sourceKey)
            .metadata(metadata)
            .build();
    }

    public static RewardPointsTransactionDto toRewardPointsTransactionDto(String userId, long eventId, Long shiftPlanId, Long positionSlotId,
                                                                          int pointsSnapshot, String sourceKey, Map<String, Object> metadata) {
        return RewardPointsTransactionDto.builder()
            .userId(userId)
            .eventId(eventId)
            .shiftPlanId(shiftPlanId)
            .positionSlotId(positionSlotId)
            .pointsSnapshot(pointsSnapshot)
            .sourceKey(sourceKey)
            .metadata(metadata)
            .build();
    }

    public static RewardPointsShareTokenDto toRewardPointsShareTokenDto(@NonNull RewardPointsShareToken rewardPointsShareToken) {
        return RewardPointsShareTokenDto.builder()
            .id(String.valueOf(rewardPointsShareToken.getId()))
            .token(rewardPointsShareToken.getToken())
            .name(rewardPointsShareToken.getName())
            .createdAt(rewardPointsShareToken.getCreatedAt())
            .build();
    }

    public static Collection<RewardPointsShareTokenDto> toRewardPointsShareTokenDto(Collection<RewardPointsShareToken> rewardPointsShareTokens) {
        return rewardPointsShareTokens.stream()
            .map(RewardPointsMapper::toRewardPointsShareTokenDto)
            .toList();
    }
}
