package at.control.shiftcontrol.shift.service.rewardpoints;

import java.util.Collection;

import at.shiftcontrol.lib.entity.Assignment;
import at.shiftcontrol.lib.exception.ConflictException;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsExportDto;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsShareTokenCreateRequestDto;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsShareTokenDto;

public interface RewardPointsService {
    void onAssignmentAccepted(Assignment assignment) throws ConflictException;

    void onAssignmentCreated(Assignment assignment, String acceptedRewardPointsHash) throws ConflictException;

    void onAssignmentRemoved(Assignment assignment);

    void onAssignmentReassignedAuction(Assignment oldAssignment, Assignment newAssignment, String acceptedRewardPointsHash)
        throws ConflictException;

    void onAssignmentReassignedTrade(Assignment offeringAssignment, Assignment requestedAssignment)
        throws ConflictException;

    void manualAdjust(String volunteerId, long eventId, int points, String reason);

    Collection<RewardPointsShareTokenDto> getAllRewardPointsShareTokens();

    Collection<RewardPointsExportDto> getRewardPointsWithShareToken(String token);

    RewardPointsShareTokenDto createRewardPointsShareToken(RewardPointsShareTokenCreateRequestDto requestDto);

    void deleteRewardPointsShareToken(long id);
}
