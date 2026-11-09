package at.control.shiftcontrol.shift.service;

import java.util.List;
import lombok.NonNull;

import at.control.shiftcontrol.shift.dto.PaginationDto;
import at.control.shiftcontrol.shift.dto.shift.ShiftDetailsDto;
import at.control.shiftcontrol.shift.dto.shift.ShiftDto;
import at.control.shiftcontrol.shift.dto.shift.ShiftModificationDto;

public interface ShiftService {
    @NonNull ShiftDetailsDto getShiftDetails(long shiftId, @NonNull String userId);

    @NonNull PaginationDto<ShiftDto> getAllOpenShiftsOfPlanPaginated(long planId, int page, int size);

    @NonNull ShiftDto createShift(long shiftPlanId, @NonNull ShiftModificationDto modificationDto);

    @NonNull ShiftDto updateShift(long shiftId, @NonNull ShiftModificationDto modificationDto);

    void deleteShift(long shiftId);

    List<Long> getShiftIds(long shiftPlanId);
}
