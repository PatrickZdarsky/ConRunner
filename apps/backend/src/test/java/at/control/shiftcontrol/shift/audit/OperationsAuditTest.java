package at.control.shiftcontrol.shift.audit;

import at.control.shiftcontrol.audit.api.AuditEntry;
import at.control.shiftcontrol.audit.api.AuditRecorder;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.ShiftPlanInvite;
import at.control.shiftcontrol.shift.lib.type.ShiftPlanInviteType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OperationsAuditTest {
    @Test void invitationAuditNeverIncludesBearerCode() {
        var recorder = mock(AuditRecorder.class);
        var audit = new OperationsAudit(recorder);
        var invite = ShiftPlanInvite.builder().id(23L).shiftPlan(ShiftPlan.builder().id(10).build())
            .type(ShiftPlanInviteType.VOLUNTEER_JOIN).code("sensitive-invite-code").active(true).build();
        audit.invite(invite, "created", null, true);
        @SuppressWarnings("unchecked") var changes = (ArgumentCaptor<java.util.List<AuditEntry.Change>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(java.util.List.class);
        verify(recorder).record(eq("shift"), eq("invite.created"), eq("invite"), eq("23"), isNull(), eq("Plan invitation created"), changes.capture());
        assertThat(changes.getValue()).contains(new AuditEntry.Change("active", null, "true"));
        assertThat(changes.getValue().toString()).doesNotContain("sensitive-invite-code");
    }
}
