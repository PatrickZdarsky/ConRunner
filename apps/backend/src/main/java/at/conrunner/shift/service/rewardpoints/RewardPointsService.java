package at.conrunner.shift.service.rewardpoints;

import java.util.UUID;
import java.util.Collection;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.platform.exception.ConflictException;
import at.conrunner.shift.dto.rewardpoints.RewardPointsExportDto;
import at.conrunner.shift.dto.rewardpoints.RewardPointsShareTokenCreateRequestDto;
import at.conrunner.shift.dto.rewardpoints.RewardPointsShareTokenDto;

public interface RewardPointsService {
    void onAssignmentAccepted(Assignment assignment) throws ConflictException;

    void onAssignmentCreated(Assignment assignment, String acceptedRewardPointsHash) throws ConflictException;

    void onAssignmentRemoved(Assignment assignment);

    void onAssignmentReassignedAuction(Assignment oldAssignment, Assignment newAssignment, String acceptedRewardPointsHash)
        throws ConflictException;

    void onAssignmentReassignedTrade(Assignment offeringAssignment, Assignment requestedAssignment)
        throws ConflictException;

    void manualAdjust(UUID volunteerId, long eventId, int points, String reason);

    Collection<RewardPointsShareTokenDto> getAllRewardPointsShareTokens();

    Collection<RewardPointsExportDto> getRewardPointsWithShareToken(String token);

    RewardPointsShareTokenDto createRewardPointsShareToken(RewardPointsShareTokenCreateRequestDto requestDto);

    void deleteRewardPointsShareToken(long id);
}
