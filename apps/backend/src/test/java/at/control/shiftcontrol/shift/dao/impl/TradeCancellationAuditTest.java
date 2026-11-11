package at.control.shiftcontrol.shift.dao.impl;

import at.control.shiftcontrol.shift.audit.OperationsAudit;
import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.lib.type.TradeStatus;
import at.control.shiftcontrol.shift.repo.AssignmentSwitchRequestRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TradeCancellationAuditTest {
    @Test void recordsEachChangedTradeAndSkipsAlreadyCancelled() {
        var repository = mock(AssignmentSwitchRequestRepository.class);
        var audit = mock(OperationsAudit.class);
        var dao = new AssignmentSwitchRequestDaoImpl(repository, audit);
        var user = UUID.randomUUID();
        var first = AssignmentSwitchRequest.builder().id(1).status(TradeStatus.OPEN).build();
        var unchanged = AssignmentSwitchRequest.builder().id(2).status(TradeStatus.CANCELED).build();
        var before = new OperationsAudit.TradeSnapshot("OPEN", "10", "11");
        when(repository.lockTradesForAssignment(12, user)).thenReturn(List.of(first, unchanged));
        when(audit.snapshot(first)).thenReturn(before);
        dao.cancelTradesForPositionSlot(12L, user);
        assertThat(first.getStatus()).isEqualTo(TradeStatus.CANCELED);
        verify(audit).trade(first, "cancelled", before);
        verify(audit, never()).trade(eq(unchanged), anyString(), any());
    }
}
