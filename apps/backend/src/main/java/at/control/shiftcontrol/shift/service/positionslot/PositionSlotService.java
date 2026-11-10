package at.control.shiftcontrol.shift.service.positionslot;

import java.util.UUID;
import java.util.Collection;

import lombok.NonNull;

import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotModificationDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotRequestDto;

public interface PositionSlotService {
    PositionSlotDto findById(@NonNull Long id);

    AssignmentDto join(@NonNull Long positionSlotId, @NonNull UUID currentUserId, @NonNull PositionSlotRequestDto requestDto);

    void leave(@NonNull Long positionSlotId, @NonNull UUID currentUserId);

    AssignmentDto joinRequest(@NonNull Long positionSlotId, @NonNull UUID currentUserId);

    void leaveRequest(@NonNull Long positionSlotId, @NonNull UUID currentUserId);

    void joinRequestWithdraw(@NonNull Long positionSlotId, @NonNull UUID currentUserId);

    void leaveRequestWithdraw(@NonNull Long positionSlotId, @NonNull UUID currentUserId);

    Collection<AssignmentDto> getAssignments(@NonNull Long positionSlotId);

    void setPreference(@NonNull UUID currentUserId, long positionSlotId, int preference);

    int getPreference(@NonNull UUID currentUserId, long positionSlotId);

    AssignmentDto getUserAssignment(@NonNull Long positionSlotId, @NonNull UUID volunteerId);

    AssignmentDto createAuction(@NonNull Long positionSlotId, @NonNull UUID currentUserId);

    AssignmentDto claimAuction(@NonNull Long positionSlotId, @NonNull UUID offeringUserId, @NonNull UUID currentUserId,
                               @NonNull PositionSlotRequestDto requestDto);

    AssignmentDto cancelAuction(@NonNull Long positionSlotId, @NonNull UUID currentUserId);

    PositionSlotDto createPositionSlot(@NonNull Long shiftId, @NonNull PositionSlotModificationDto modificationDto);

    PositionSlotDto updatePositionSlot(@NonNull Long positionSlotId, @NonNull PositionSlotModificationDto modificationDto);

    void deletePositionSlot(@NonNull Long positionSlotId);
}
