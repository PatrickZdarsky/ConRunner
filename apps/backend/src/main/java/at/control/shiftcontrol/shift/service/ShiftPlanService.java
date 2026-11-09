package at.control.shiftcontrol.shift.service;

import java.util.Collection;
import lombok.NonNull;

import at.shiftcontrol.lib.type.LockStatus;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanInviteCreateRequestDto;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanInviteCreateResponseDto;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanInviteDetailsDto;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanInviteDto;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanJoinRequestDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanCreateDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanModificationDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanStatisticsDto;

public interface ShiftPlanService {
    @NonNull Collection<ShiftPlanDto> getAllOfEvent(long eventId);

    @NonNull Collection<ShiftPlanDto> getAll();

    @NonNull ShiftPlanDto get(long shiftPlanId);

    @NonNull ShiftPlanCreateDto createShiftPlan(long eventId, @NonNull ShiftPlanModificationDto modificationDto);

    @NonNull ShiftPlanDto update(long shiftPlanId, @NonNull ShiftPlanModificationDto modificationDto);

    void delete(long shiftPlanId);

    @NonNull ShiftPlanInviteCreateResponseDto createShiftPlanInviteCode(long shiftPlanId, @NonNull ShiftPlanInviteCreateRequestDto requestDto);

    void revokeShiftPlanInvite(long inviteId);

    void deleteShiftPlanInvite(long inviteId);

    @NonNull ShiftPlanInviteDetailsDto getShiftPlanInviteDetails(@NonNull String inviteCode);

    @NonNull Collection<ShiftPlanInviteDto> getAllShiftPlanInvites(long shiftPlanId);

    void joinShiftPlan(@NonNull ShiftPlanJoinRequestDto requestDto);

    void updateLockStatus(long shiftPlanId, @NonNull LockStatus lockStatus);

    void leavePlan(long shiftPlanId);

    @NonNull ShiftPlanStatisticsDto getShiftPlanStatistics(long shiftPlanId);
}
