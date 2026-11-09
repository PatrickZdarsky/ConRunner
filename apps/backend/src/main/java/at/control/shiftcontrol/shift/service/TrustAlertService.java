package at.control.shiftcontrol.shift.service;

import at.shiftcontrol.lib.dto.PaginationDto;
import at.shiftcontrol.lib.dto.TrustAlertDto;
import at.shiftcontrol.shiftservice.dto.TrustAlertDisplayDto;

import lombok.NonNull;

public interface TrustAlertService {
    @NonNull PaginationDto<TrustAlertDisplayDto> getAllPaginated(int  page, int size);

    @NonNull TrustAlertDisplayDto save(@NonNull TrustAlertDto alert);

    void delete(long id);
}
