package at.conrunner.shift.service.impl;

import at.conrunner.audit.api.AuditEntry;
import at.conrunner.audit.api.AuditRecorder;
import at.conrunner.communications.api.NotificationRequests;
import at.conrunner.identity.UserService;
import at.conrunner.shift.dao.TrustAlertDao;
import at.conrunner.shift.entity.TrustAlert;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.type.TrustAlertType;
import at.conrunner.shift.mapper.TrustAlertAssemblingMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import static org.mockito.Mockito.*;

class TrustAlertAuditTest {
    @Test void deleteRecordsTheAffectedUserAndTypeBeforeDeleting() {
        var recorder = mock(AuditRecorder.class);
        var dao = mock(TrustAlertDao.class);
        var userId = UUID.randomUUID();
        var entity = TrustAlert.builder().id(17).volunteer(Volunteer.builder().id(userId).build()).alertType(TrustAlertType.SPAM).build();
        when(dao.findById(17)).thenReturn(Optional.of(entity));
        var service = new TrustAlertServiceImpl(recorder, dao, mock(TrustAlertAssemblingMapper.class),
            mock(ApplicationEventPublisher.class), mock(NotificationRequests.class), mock(UserService.class));
        service.delete(17);
        var order = inOrder(recorder, dao);
        order.verify(dao).findById(17);
        order.verify(recorder).record("shift", "trust_alert.deleted", "trust_alert", "17", null, "Trust alert deleted",
            List.of(new AuditEntry.Change("volunteerId", userId.toString(), null), new AuditEntry.Change("type", "SPAM", null)));
        order.verify(dao).delete(17);
    }
}
