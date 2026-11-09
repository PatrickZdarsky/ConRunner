package at.control.shiftcontrol.shift.service.impl;

import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.dto.PaginationDto;
import at.control.shiftcontrol.shift.dto.TrustAlertDto;
import at.control.shiftcontrol.shift.entity.TrustAlert;
import at.control.shiftcontrol.shift.lib.event.events.TrustAlertEvent;
import at.control.shiftcontrol.identity.AdminOnly;
import at.control.shiftcontrol.shift.dao.TrustAlertDao;
import at.control.shiftcontrol.shift.dto.TrustAlertDisplayDto;
import at.control.shiftcontrol.shift.mapper.PaginationMapper;
import at.control.shiftcontrol.shift.mapper.TrustAlertAssemblingMapper;
import at.control.shiftcontrol.shift.service.TrustAlertService;

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
