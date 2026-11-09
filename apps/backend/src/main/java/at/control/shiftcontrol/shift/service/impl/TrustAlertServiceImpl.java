package at.control.shiftcontrol.shift.service.impl;

import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.dto.PaginationDto;
import at.shiftcontrol.lib.dto.TrustAlertDto;
import at.shiftcontrol.lib.entity.TrustAlert;
import at.shiftcontrol.lib.event.events.TrustAlertEvent;
import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.dao.TrustAlertDao;
import at.shiftcontrol.shiftservice.dto.TrustAlertDisplayDto;
import at.shiftcontrol.shiftservice.mapper.PaginationMapper;
import at.shiftcontrol.shiftservice.mapper.TrustAlertAssemblingMapper;
import at.shiftcontrol.shiftservice.service.TrustAlertService;

@Service
@RequiredArgsConstructor
public class TrustAlertServiceImpl implements TrustAlertService {
    private final TrustAlertDao trustAlertDao;
    private final TrustAlertAssemblingMapper trustAlertAssemblingMapper;
    private final ApplicationEventPublisher publisher;

    @Override
    @AdminOnly
    public @NonNull PaginationDto<TrustAlertDisplayDto> getAllPaginated(int page, int size) {
        var items = trustAlertAssemblingMapper.toDto(trustAlertDao.getAllPaginated(page, size));
        long totalItems = trustAlertDao.findAllSize();
        return PaginationMapper.toPaginationDto(size, page, totalItems, items);
    }

    @Override
    @AdminOnly
    public @NonNull TrustAlertDisplayDto save(@NonNull TrustAlertDto alert) {
        TrustAlert entity = trustAlertAssemblingMapper.toEntity(alert);
        entity = trustAlertDao.save(entity);

        publisher.publishEvent(TrustAlertEvent.alertReceived(entity));

        return trustAlertAssemblingMapper.toDto(entity);
    }

    @Override
    @AdminOnly
    public void delete(long id) {
        trustAlertDao.delete(id);
    }
}
