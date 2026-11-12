package at.conrunner.shift.service.positionslot;

import at.conrunner.communications.api.NotificationRequest;
import at.conrunner.communications.api.NotificationContext;
import at.conrunner.communications.api.NotificationRequests;
import at.conrunner.shift.audit.PlanningAudit;
import at.conrunner.shift.dao.AssignmentDao;
import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.lib.type.AssignmentStatus;
import at.conrunner.shift.service.AssignmentService;
import at.conrunner.shift.service.EligibilityService;
import at.conrunner.shift.service.positionslot.impl.PlannerPositionSlotServiceImpl;
import at.conrunner.shift.util.SecurityHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlannerPositionSlotNotificationTest {
    @Mock
    PlanningAudit planningAudit;
    @Mock AssignmentDao assignments;
    @Mock AssignmentService assignmentService;
    @Mock SecurityHelper security;
    @Mock EligibilityService eligibility;
    @Mock ApplicationEventPublisher legacyPublisher;
    @Mock NotificationRequests notifications;
    @InjectMocks PlannerPositionSlotServiceImpl service;

    @Test
    void acceptingJoinRequestsNotifiesTheAffectedVolunteer() {
        var userId = UUID.randomUUID();
        var assignment = mock(Assignment.class);
        var slot = mock(PositionSlot.class);
        var shift = mock(Shift.class);
        var plan = mock(ShiftPlan.class);
        var event = mock(Event.class);
        when(assignments.getAssignmentForPositionSlotAndUser(5, userId)).thenReturn(assignment);
        when(assignment.getPositionSlot()).thenReturn(slot);
        when(assignment.getStatus()).thenReturn(AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
        when(eligibility.hasCapacity(slot)).thenReturn(true);
        when(slot.getShift()).thenReturn(shift);
        when(slot.getName()).thenReturn("Reception");
        when(shift.getShiftPlan()).thenReturn(plan);
        when(plan.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(42L);
        service.acceptRequest(3, 5, userId);
        var request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notifications).request(request.capture());
        assertThat(request.getValue().recipientId()).isEqualTo(userId);
        assertThat(request.getValue().type()).isEqualTo("shift.assignment.join.accepted");
        assertThat(NotificationContext.decode(request.getValue().templateData()))
            .isEqualTo(new NotificationContext("42", "Reception", ""));
    }
}
