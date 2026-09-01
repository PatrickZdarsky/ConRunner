package at.control.shiftcontrol.shift.positionslot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
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
class StaffingShiftServiceTest {
    private static final Instant EVENT_START = Instant.parse("2026-05-12T07:00:00Z");
    private static final Instant EVENT_END = Instant.parse("2026-05-12T22:00:00Z");

    @Mock
    private StaffingShiftRepository staffingShiftRepository;

    @Mock
    private ShiftPlanService shiftPlanService;

    @Mock
    private PlanAccess planAccess;

    @Mock
    private EventCatalogQuery eventCatalogQuery;

    @Mock
    private Clock clock;

    @InjectMocks
    private StaffingShiftService shifts;

    @Test
    void aStandaloneShiftKeepsItsDirectLocation() {
        UUID planId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        configurePlan(planId, eventId, StaffingMode.SUPERVISED);
        when(eventCatalogQuery.requireEvent(eventId)).thenReturn(new EventCatalogQuery.CatalogEvent(eventId, EVENT_START, EVENT_END));
        when(eventCatalogQuery.requireLocation(locationId)).thenReturn(new EventCatalogQuery.CatalogLocation(locationId, eventId));
        when(clock.instant()).thenReturn(EVENT_START);
        when(staffingShiftRepository.save(any(StaffingShiftEntity.class))).thenAnswer(invocation -> {
            StaffingShiftEntity shift = invocation.getArgument(0);
            shift.setId(shiftId);
            return shift;
        });

        StaffingShiftDetails result = shifts.create(planId, data(locationId, null, ShiftModeOverride.INHERIT));

        assertEquals(locationId, result.locationId());
        assertEquals(null, result.sessionId());
        assertEquals(StaffingMode.SUPERVISED, result.effectiveMode());
        verify(planAccess).requirePlanner(planId);
    }

    @Test
    void aSessionLinkedShiftAlwaysDerivesItsLocationFromTheSession() {
        UUID planId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID sessionLocationId = UUID.randomUUID();
        configurePlan(planId, eventId, StaffingMode.SELF_SIGNUP);
        when(eventCatalogQuery.requireEvent(eventId)).thenReturn(new EventCatalogQuery.CatalogEvent(eventId, EVENT_START, EVENT_END));
        when(eventCatalogQuery.requireSession(sessionId)).thenReturn(new EventCatalogQuery.CatalogSession(sessionId, eventId, sessionLocationId));
        when(clock.instant()).thenReturn(EVENT_START);
        when(staffingShiftRepository.save(any(StaffingShiftEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffingShiftDetails result = shifts.create(planId, data(null, sessionId, ShiftModeOverride.LOCKED));

        assertEquals(sessionLocationId, result.locationId());
        assertEquals(sessionId, result.sessionId());
        assertEquals(ShiftModeOverride.LOCKED, result.modeOverride());
        assertEquals(StaffingMode.LOCKED, result.effectiveMode());
    }

    @Test
    void rejectsAStandaloneLocationOrTimeOutsideThePlanEvent() {
        UUID planId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID otherEventId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        configurePlan(planId, eventId, StaffingMode.SELF_SIGNUP);
        when(eventCatalogQuery.requireEvent(eventId)).thenReturn(new EventCatalogQuery.CatalogEvent(eventId, EVENT_START, EVENT_END));
        when(eventCatalogQuery.requireLocation(locationId)).thenReturn(new EventCatalogQuery.CatalogLocation(locationId, otherEventId));

        assertThrows(ShiftValidationException.class, () -> shifts.create(planId, data(locationId, null, ShiftModeOverride.INHERIT)));
        assertThrows(ShiftValidationException.class, () -> shifts.create(planId,
            new StaffingShiftData("Early", null, null, EVENT_START.minusSeconds(1), EVENT_START.plusSeconds(60), null, null,
                ShiftModeOverride.INHERIT)));
    }

    @Test
    void inputDoesNotAllowAmbiguousDirectAndSessionLocations() {
        assertThrows(ShiftValidationException.class, () -> new StaffingShiftData("Desk", null, null, EVENT_START,
            EVENT_START.plusSeconds(60), UUID.randomUUID(), UUID.randomUUID(), ShiftModeOverride.INHERIT));
    }

    private void configurePlan(UUID planId, UUID eventId, StaffingMode mode) {
        when(shiftPlanService.get(planId)).thenReturn(new ShiftPlanDetails(planId, eventId, "Operations", null, null, mode,
            EVENT_START, null));
    }

    private static StaffingShiftData data(UUID locationId, UUID sessionId, ShiftModeOverride override) {
        return new StaffingShiftData("Information desk", "Help", null, EVENT_START.plusSeconds(60), EVENT_START.plusSeconds(120),
            locationId, sessionId, override);
    }
}
