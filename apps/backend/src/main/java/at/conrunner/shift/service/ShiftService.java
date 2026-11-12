package at.conrunner.shift.service;

import java.util.UUID;
import java.util.List;
import lombok.NonNull;

import at.conrunner.shift.dto.PaginationDto;
import at.conrunner.shift.dto.shift.ShiftDetailsDto;
import at.conrunner.shift.dto.shift.ShiftDto;
import at.conrunner.shift.dto.shift.ShiftModificationDto;

public interface ShiftService {
    @NonNull ShiftDetailsDto getShiftDetails(long shiftId, @NonNull UUID userId);

    @NonNull PaginationDto<ShiftDto> getAllOpenShiftsOfPlanPaginated(long planId, int page, int size);

    @NonNull ShiftDto createShift(long shiftPlanId, @NonNull ShiftModificationDto modificationDto);

    @NonNull ShiftDto updateShift(long shiftId, @NonNull ShiftModificationDto modificationDto);

    void deleteShift(long shiftId);

    List<Long> getShiftIds(long shiftPlanId);
}
