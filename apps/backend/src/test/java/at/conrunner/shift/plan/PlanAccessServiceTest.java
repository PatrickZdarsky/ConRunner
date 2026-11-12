package at.conrunner.shift.plan;

import at.conrunner.identity.AuthenticatedActor;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.auth.UserAttributeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanAccessServiceTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void membershipAndPlannerStatusRemainScopedToEachPlan() {
        var userId = UUID.randomUUID();
        var principal = mock(AuthenticatedActor.class);
        var attributes = mock(UserAttributeProvider.class);
        when(principal.userId()).thenReturn(Optional.of(userId));
        when(attributes.getPlansWhereUserIsVolunteer(userId)).thenReturn(Set.of(42L));
        when(attributes.getPlansWhereUserIsPlanner(userId)).thenReturn(Set.of(43L));
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));

        var actor = new CurrentShiftActor(attributes).get();
        assertTrue(actor.isVolunteerInPlan(42L));
        assertFalse(actor.isPlannerInPlan(42L));
        assertTrue(actor.isPlannerInPlan(43L));
        assertFalse(actor.isVolunteerInPlan(43L));
    }
}
