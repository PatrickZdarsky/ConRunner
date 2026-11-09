package at.control.shiftcontrol.shift.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import at.control.shiftcontrol.audit.AuditLog;
import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftPlanServiceTest {
    @Mock private ShiftPlanRepository shiftPlanRepository;
    @Mock private PlanMembershipRepository membershipRepository;
    @Mock private EventCatalogQuery eventCatalogQuery;
    @Mock private PlanInviteService planInviteService;
    @Mock private PlanAccess planAccess;
    @Mock private AuditLog auditLog;
    @Mock private Clock clock;
    @InjectMocks private ShiftPlanService plans;

    @Test
    void createsTheLegacyDefaultVolunteerAndPlannerInvites() {
        UUID eventId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-05T10:00:00Z");
        when(clock.instant()).thenReturn(now);
        when(shiftPlanRepository.save(any(ShiftPlanEntity.class))).thenAnswer(invocation -> {
            ShiftPlanEntity plan = invocation.getArgument(0);
            plan.setId(planId);
            return plan;
        });
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(actorId, true));
        IssuedPlanInvite volunteerInvite = new IssuedPlanInvite(UUID.randomUUID(), "volunteer-code");
        IssuedPlanInvite plannerInvite = new IssuedPlanInvite(UUID.randomUUID(), "planner-code");
        when(planInviteService.createDefaultInvite(planId, PlanInviteType.VOLUNTEER, now)).thenReturn(volunteerInvite);
        when(planInviteService.createDefaultInvite(planId, PlanInviteType.PLANNER, now)).thenReturn(plannerInvite);

        CreatedShiftPlan result = plans.create(eventId, new ShiftPlanData(" Crew ", " short ", " long "));

        assertEquals(planId, result.shiftPlan().id());
        assertEquals("Crew", result.shiftPlan().name());
        assertEquals(StaffingMode.SELF_SIGNUP, result.shiftPlan().defaultStaffingMode());
        assertEquals(volunteerInvite, result.volunteerInvite());
        assertEquals(plannerInvite, result.plannerInvite());
        verify(planAccess).requirePlatformAdmin();
        verify(eventCatalogQuery).requireEvent(eventId);
        verify(planInviteService).createDefaultInvite(planId, PlanInviteType.VOLUNTEER, now);
        verify(planInviteService).createDefaultInvite(planId, PlanInviteType.PLANNER, now);
        verify(auditLog).record(actorId, "SHIFT_PLAN_CREATED", "SHIFT_PLAN", planId, null, "SELF_SIGNUP", null);
    }
}
