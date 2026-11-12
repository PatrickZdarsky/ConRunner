package at.conrunner.shift.service.impl;

import at.conrunner.communications.api.NotificationTypes;
import at.conrunner.audit.api.AuditRecorder;
import at.conrunner.audit.api.AuditEntry;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import at.conrunner.communications.api.NotificationRequest;
import at.conrunner.communications.api.NotificationRequests;
import at.conrunner.identity.UserService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import at.conrunner.shift.dto.PaginationDto;
import at.conrunner.shift.dto.TrustAlertDto;
import at.conrunner.shift.entity.TrustAlert;
import at.conrunner.shift.lib.event.events.TrustAlertEvent;
import at.conrunner.identity.AdminOnly;
import at.conrunner.shift.dao.TrustAlertDao;
import at.conrunner.shift.dto.TrustAlertDisplayDto;
import at.conrunner.shift.mapper.PaginationMapper;
import at.conrunner.shift.mapper.TrustAlertAssemblingMapper;
import at.conrunner.shift.service.TrustAlertService;

@Service
@RequiredArgsConstructor
public class TrustAlertServiceImpl implements TrustAlertService {
    private final AuditRecorder auditRecorder;
    private final TrustAlertDao trustAlertDao;
    private final TrustAlertAssemblingMapper trustAlertAssemblingMapper;
    private final ApplicationEventPublisher publisher;
    private final NotificationRequests notificationRequests;
    private final UserService identityUsers;

    @Override
    @AdminOnly
    public @NonNull PaginationDto<TrustAlertDisplayDto> getAllPaginated(int page, int size) {
        var items = trustAlertAssemblingMapper.toDto(trustAlertDao.getAllPaginated(page, size));
        long totalItems = trustAlertDao.findAllSize();
        return PaginationMapper.toPaginationDto(size, page, totalItems, items);
    }

    @Override
    @AdminOnly
    @Transactional
    public @NonNull TrustAlertDisplayDto save(@NonNull TrustAlertDto alert) {
        TrustAlert entity = trustAlertAssemblingMapper.toEntity(alert);
        entity = trustAlertDao.save(entity);
        record(entity, true);

        var triggerId = java.util.UUID.randomUUID();
        identityUsers.getPlatformAdmins().forEach(admin -> notificationRequests.request(
            new NotificationRequest(triggerId, admin.getId(), NotificationTypes.TRUST_ALERT, "global")));

        publisher.publishEvent(TrustAlertEvent.alertReceived(entity));

        return trustAlertAssemblingMapper.toDto(entity);
    }

    @Override
    @AdminOnly
    @Transactional
    public void delete(long id) {
        trustAlertDao.findById(id).ifPresent(entity -> {
            record(entity, false);
            trustAlertDao.delete(id);
        });
    }
    private void record(TrustAlert entity, boolean created) {
        String userId = entity.getVolunteer().getId().toString();
        String type = entity.getAlertType().name();
        auditRecorder.record("shift", created ? "trust_alert.created" : "trust_alert.deleted", "trust_alert",
            Long.toString(entity.getId()), null, created ? "Trust alert created" : "Trust alert deleted",
            List.of(new AuditEntry.Change("volunteerId", created ? null : userId, created ? userId : null),
                new AuditEntry.Change("type", created ? null : type, created ? type : null)));
    }
}
