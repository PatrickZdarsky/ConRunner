package at.conrunner.shift.service;

import at.conrunner.shift.dto.PaginationDto;
import at.conrunner.shift.dto.TrustAlertDto;
import at.conrunner.shift.dto.TrustAlertDisplayDto;

import lombok.NonNull;

public interface TrustAlertService {
    @NonNull PaginationDto<TrustAlertDisplayDto> getAllPaginated(int  page, int size);

    @NonNull TrustAlertDisplayDto save(@NonNull TrustAlertDto alert);

    void delete(long id);
}
