package at.control.shiftcontrol.shift.service.positionslot;

import java.util.Collection;

import lombok.NonNull;

import at.shiftcontrol.shiftservice.dto.assignment.AssignmentDto;
import at.shiftcontrol.shiftservice.dto.positionslot.PositionSlotDto;
import at.shiftcontrol.shiftservice.dto.positionslot.PositionSlotModificationDto;
import at.shiftcontrol.shiftservice.dto.positionslot.PositionSlotRequestDto;

public interface PositionSlotService {
    PositionSlotDto findById(@NonNull Long id);

    AssignmentDto join(@NonNull Long positionSlotId, @NonNull String currentUserId, @NonNull PositionSlotRequestDto requestDto);

    void leave(@NonNull Long positionSlotId, @NonNull String currentUserId);

    AssignmentDto joinRequest(@NonNull Long positionSlotId, @NonNull String currentUserId);

    void leaveRequest(@NonNull Long positionSlotId, @NonNull String currentUserId);

    void joinRequestWithdraw(@NonNull Long positionSlotId, @NonNull String currentUserId);

    void leaveRequestWithdraw(@NonNull Long positionSlotId, @NonNull String currentUserId);

    Collection<AssignmentDto> getAssignments(@NonNull Long positionSlotId);

    void setPreference(@NonNull String currentUserId, long positionSlotId, int preference);

    int getPreference(@NonNull String currentUserId, long positionSlotId);

    AssignmentDto getUserAssignment(@NonNull Long positionSlotId, @NonNull String volunteerId);

    AssignmentDto createAuction(@NonNull Long positionSlotId, @NonNull String currentUserId);

    AssignmentDto claimAuction(@NonNull Long positionSlotId, @NonNull String offeringUserId, @NonNull String currentUserId,
                               @NonNull PositionSlotRequestDto requestDto);

    AssignmentDto cancelAuction(@NonNull Long positionSlotId, @NonNull String currentUserId);

    PositionSlotDto createPositionSlot(@NonNull Long shiftId, @NonNull PositionSlotModificationDto modificationDto);

    PositionSlotDto updatePositionSlot(@NonNull Long positionSlotId, @NonNull PositionSlotModificationDto modificationDto);

    void deletePositionSlot(@NonNull Long positionSlotId);
}
