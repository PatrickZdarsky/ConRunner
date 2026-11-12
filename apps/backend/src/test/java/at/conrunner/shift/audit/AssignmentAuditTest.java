package at.conrunner.shift.audit;

import at.conrunner.audit.api.AuditContext;
import at.conrunner.audit.api.AuditEntry;
import at.conrunner.audit.api.AuditRecorder;
import at.conrunner.audit.api.AuditWriter;
import at.conrunner.identity.User;
import at.conrunner.identity.UserService;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.auth.ShiftActor;
import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.type.AssignmentStatus;
import io.micrometer.tracing.Tracer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssignmentAuditTest {
    @Test void capturesApprovedAssignmentFactsAndHumanActor() {
        AuditWriter writer = mock(AuditWriter.class);
        CurrentShiftActor current = mock(CurrentShiftActor.class);
        ShiftActor actor = mock(ShiftActor.class);
        @SuppressWarnings("unchecked") ObjectProvider<Tracer> tracers = mock(ObjectProvider.class);
        UUID user = UUID.randomUUID();
        when(current.find()).thenReturn(Optional.of(actor));
        when(actor.requireUserId()).thenReturn(user);
        when(actor.getName()).thenReturn("Alex");
        UserService users = mock(UserService.class);
        when(users.getUsers(java.util.List.of(user))).thenReturn(java.util.List.of(User.builder().username("Alex").build()));
        var context = org.mockito.Mockito.mock(AuditContext.class);
        org.mockito.Mockito.when(context.actor()).thenReturn(new AuditEntry.Actor(AuditEntry.ActorKind.HUMAN, user, null, "Alex"));
        var audit = new AssignmentAudit(new AuditRecorder(writer, context), users);
        var volunteer = mock(Volunteer.class);
        when(volunteer.getId()).thenReturn(user);
        var slot = mock(PositionSlot.class);
        when(slot.getId()).thenReturn(42L);
        when(slot.getName()).thenReturn("Reception");
        var assignment = Assignment.builder().id(7).positionSlot(slot).assignedVolunteer(volunteer)
            .status(AssignmentStatus.ACCEPTED).acceptedRewardPoints(5).build();
        audit.joined(assignment);
        var captured = org.mockito.ArgumentCaptor.forClass(AuditEntry.class);
        verify(writer).record(captured.capture());
        var entry = captured.getValue();
        assertThat(entry.actor().userId()).isEqualTo(user);
        assertThat(entry.subjectId()).isEqualTo("7");
        assertThat(entry.subjectLabel()).isEqualTo("Reception");
        assertThat(entry.changes()).containsExactly(
            new AuditEntry.Change("positionSlotId", null, "42"),
            new AuditEntry.Change("volunteerId", null, user.toString()),
            new AuditEntry.Change("volunteerName", null, "Alex"),
            new AuditEntry.Change("status", null, "ACCEPTED"),
            new AuditEntry.Change("acceptedRewardPoints", null, "5"));
        var before = audit.snapshot(assignment);
        assignment.setStatus(AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN);
        audit.leaveRequested(assignment, before);
        verify(users, times(2)).getUsers(java.util.List.of(user));

    }
}
