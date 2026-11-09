package at.control.shiftcontrol.shift.service;

import java.util.Collection;
import lombok.NonNull;

import at.control.shiftcontrol.shift.lib.type.LockStatus;
import at.control.shiftcontrol.shift.dto.invite.ShiftPlanInviteCreateRequestDto;
import at.control.shiftcontrol.shift.dto.invite.ShiftPlanInviteCreateResponseDto;
import at.control.shiftcontrol.shift.dto.invite.ShiftPlanInviteDetailsDto;
import at.control.shiftcontrol.shift.dto.invite.ShiftPlanInviteDto;
import at.control.shiftcontrol.shift.dto.invite.ShiftPlanJoinRequestDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanCreateDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanModificationDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanStatisticsDto;

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
