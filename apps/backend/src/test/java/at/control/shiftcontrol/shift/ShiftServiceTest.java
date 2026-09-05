package at.control.shiftcontrol.shift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.ShiftModeOverride;
import at.control.shiftcontrol.shift.plan.ShiftPlanDetails;
import at.control.shiftcontrol.shift.plan.ShiftPlanService;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {
    private static final Instant START = Instant.parse("2026-05-01T10:00:00Z");

    @Mock private ShiftRepository shiftRepository;
    @Mock private ShiftPlanService shiftPlanService;
    @Mock private PlanAccess planAccess;
    @Mock private EventCatalogQuery catalog;
    @Mock private Clock clock;
    @InjectMocks private ShiftService shifts;

    @Test
    void sessionLinkedShiftResolvesItsLocationFromTheSession() {
        UUID planId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(clock.instant()).thenReturn(START.minusSeconds(10));
        when(shiftPlanService.get(planId)).thenReturn(plan(planId, eventId));
        when(catalog.requireEvent(eventId)).thenReturn(new EventCatalogQuery.CatalogEvent(eventId, START.minusSeconds(60), START.plusSeconds(3600)));
        when(catalog.requireSession(sessionId)).thenReturn(new EventCatalogQuery.CatalogSession(sessionId, eventId, locationId));
        when(shiftRepository.save(any(ShiftEntity.class))).thenAnswer(invocation -> {
            ShiftEntity shift = invocation.getArgument(0);
            shift.setId(UUID.randomUUID());
            return shift;
        });

        ShiftDetails result = shifts.create(planId, new ShiftData("Desk", null, null, START, START.plusSeconds(600),
            null, sessionId, ShiftModeOverride.INHERIT));

        assertEquals(locationId, result.locationId());
        assertEquals(sessionId, result.sessionId());
    }

    @Test
    void directLocationAndSessionAreMutuallyExclusive() {
        assertThrows(ShiftValidationException.class, () -> new ShiftData("Desk", null, null, START, START.plusSeconds(60),
            UUID.randomUUID(), UUID.randomUUID(), ShiftModeOverride.INHERIT));
    }

    @Test
    void standaloneShiftKeepsItsDirectLocation() {
        UUID planId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(clock.instant()).thenReturn(START);
        when(shiftPlanService.get(planId)).thenReturn(plan(planId, eventId));
        when(catalog.requireEvent(eventId)).thenReturn(new EventCatalogQuery.CatalogEvent(eventId, START.minusSeconds(60), START.plusSeconds(3600)));
        when(catalog.requireLocation(locationId)).thenReturn(new EventCatalogQuery.CatalogLocation(locationId, eventId));
        when(shiftRepository.save(any(ShiftEntity.class))).thenAnswer(invocation -> {
            ShiftEntity shift = invocation.getArgument(0);
            shift.setId(UUID.randomUUID());
            return shift;
        });

        ShiftDetails result = shifts.create(planId, new ShiftData("Desk", null, null, START, START.plusSeconds(60),
            locationId, null, ShiftModeOverride.INHERIT));

        assertEquals(locationId, result.locationId());
    }

    private static ShiftPlanDetails plan(UUID planId, UUID eventId) {
        return new ShiftPlanDetails(planId, eventId, "Plan", null, null, StaffingMode.SELF_SIGNUP, START, null);
    }
}
