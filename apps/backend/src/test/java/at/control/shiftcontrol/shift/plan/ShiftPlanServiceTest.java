package at.control.shiftcontrol.shift.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftPlanServiceTest {
    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    @Mock
    private ShiftPlanRepository planRepository;

    @Mock
    private PlanMembershipRepository membershipRepository;

    @Mock
    private VolunteerProfileService volunteerProfileService;

    @Mock
    private EventCatalogQuery eventCatalogQuery;

    @Mock
    private PlanAccess planAccess;

    @Mock
    private PlanModeTransitionService planModeTransitionService;

    @Mock
    private Clock clock;

    @InjectMocks
    private ShiftPlanService plans;

    @Test
    void createMakesTheAdministratorAPlannerVolunteerAndDefaultsToSelfSignup() {
        UUID eventId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        when(clock.instant()).thenReturn(NOW);
        when(planRepository.existsByEventIdAndNameIgnoreCase(eventId, "Operations")).thenReturn(false);
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(creatorId, true));
        when(planRepository.save(any(ShiftPlanEntity.class))).thenAnswer(invocation -> {
            ShiftPlanEntity plan = invocation.getArgument(0);
            plan.setId(planId);
            return plan;
        });
        ShiftPlanData data = new ShiftPlanData(" Operations ", " short ", null);

        ShiftPlanDetails created = plans.create(eventId, data);

        assertEquals(planId, created.id());
        assertEquals(StaffingMode.SELF_SIGNUP, created.defaultStaffingMode());
        assertEquals("Operations", created.name());
        ArgumentCaptor<PlanMembershipEntity> membership = ArgumentCaptor.forClass(PlanMembershipEntity.class);
        verify(membershipRepository).save(membership.capture());
        assertEquals(planId, membership.getValue().getPlanId());
        assertEquals(creatorId, membership.getValue().getUserId());
        assertEquals(true, membership.getValue().isPlanner());
        verify(volunteerProfileService).ensureProfile(creatorId);
    }

    @Test
    void createRejectsCaseInsensitiveDuplicateNamesWithinTheEvent() {
        UUID eventId = UUID.randomUUID();
        when(planRepository.existsByEventIdAndNameIgnoreCase(eventId, "Operations")).thenReturn(true);

        assertThrows(ShiftConflictException.class, () -> plans.create(eventId, new ShiftPlanData("Operations", null, null)));
    }

    @Test
    void changingTheDefaultModeIsScopedToAPlanner() {
        UUID planId = UUID.randomUUID();
        ShiftPlanEntity plan = plan(planId);
        ShiftPlanDetails changed = new ShiftPlanDetails(planId, plan.getEventId(), plan.getName(), null, null,
            StaffingMode.LOCKED, plan.getCreatedAt(), null);
        when(planModeTransitionService.change(planId, StaffingMode.LOCKED)).thenReturn(changed);

        ShiftPlanDetails result = plans.changeStaffingMode(planId, StaffingMode.LOCKED);

        assertEquals(StaffingMode.LOCKED, result.defaultStaffingMode());
        verify(planModeTransitionService).change(planId, StaffingMode.LOCKED);
    }

    private static ShiftPlanEntity plan(UUID planId) {
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setEventId(UUID.randomUUID());
        plan.setName("Operations");
        plan.setDefaultStaffingMode(StaffingMode.SELF_SIGNUP);
        plan.setCreatedAt(NOW);
        return plan;
    }
}
