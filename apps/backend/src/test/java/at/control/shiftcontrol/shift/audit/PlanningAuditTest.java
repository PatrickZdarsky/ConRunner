package at.control.shiftcontrol.shift.audit;

import at.control.shiftcontrol.audit.api.AuditEntry;
import at.control.shiftcontrol.audit.api.AuditRecorder;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlanningAuditTest {
    @Test void shiftUpdatePreservesApprovedBeforeAndAfterValues() {
        var recorder = mock(AuditRecorder.class);
        var audit = new PlanningAudit(recorder);
        var shift = Shift.builder().id(4).shiftPlan(ShiftPlan.builder().id(7).build()).name("Old")
            .startTime(Instant.parse("2026-01-01T08:00:00Z")).endTime(Instant.parse("2026-01-01T09:00:00Z")).build();
        var before = audit.snapshot(shift);
        shift.setName("New");
        audit.shiftUpdated(shift, before);
        @SuppressWarnings("unchecked") var changes = (ArgumentCaptor<java.util.List<AuditEntry.Change>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(java.util.List.class);
        verify(recorder).record(eq("shift"), eq("shift.updated"), eq("shift"), eq("4"), eq("New"), eq("Shift updated"), changes.capture());
        assertThat(changes.getValue()).contains(new AuditEntry.Change("name", "Old", "New"),
            new AuditEntry.Change("parentId", "7", "7"));
    }
}
