package at.control.shiftcontrol.shift.service;

import at.control.shiftcontrol.shift.dto.PaginationDto;
import at.control.shiftcontrol.shift.dto.TrustAlertDto;
import at.control.shiftcontrol.shift.dto.TrustAlertDisplayDto;

import lombok.NonNull;

public interface TrustAlertService {
    @NonNull PaginationDto<TrustAlertDisplayDto> getAllPaginated(int  page, int size);

    @NonNull TrustAlertDisplayDto save(@NonNull TrustAlertDto alert);

    void delete(long id);
}
