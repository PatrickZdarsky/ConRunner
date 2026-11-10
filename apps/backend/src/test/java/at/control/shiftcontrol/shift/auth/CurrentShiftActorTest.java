package at.control.shiftcontrol.shift.auth;

import at.control.shiftcontrol.identity.ActorType;
import at.control.shiftcontrol.identity.AuthenticatedActor;
import at.control.shiftcontrol.platform.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentShiftActorTest {
    private final UserAttributeProvider attributes = mock(UserAttributeProvider.class);
    private final CurrentShiftActor currentActor = new CurrentShiftActor(attributes);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesHumanIdentityAndMembership() {
        var id = UUID.randomUUID();
        var principal = mock(AuthenticatedActor.class);
        when(principal.userId()).thenReturn(Optional.of(id));
        when(principal.type()).thenReturn(ActorType.HUMAN);
        when(attributes.getPlansWhereUserIsVolunteer(id)).thenReturn(java.util.Set.of(42L));
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));

        assertEquals(id, currentActor.get().requireUserId());
        assertTrue(currentActor.get().isVolunteerInPlan(42L));
    }

    @Test
    void machineHasNoHumanUserId() {
        var principal = mock(AuthenticatedActor.class);
        when(principal.userId()).thenReturn(Optional.empty());
        when(principal.type()).thenReturn(ActorType.MACHINE);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));

        assertTrue(currentActor.get().isMachine());
        assertThrows(UnauthorizedException.class, () -> currentActor.get().requireUserId());
    }

    @Test
    void missingOrUnauthenticatedContextIsRejected() {
        assertTrue(currentActor.find().isEmpty());
        assertThrows(UnauthorizedException.class, currentActor::get);
        var principal = mock(AuthenticatedActor.class);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.unauthenticated(principal, null));
        assertTrue(currentActor.find().isEmpty());
        assertThrows(UnauthorizedException.class, currentActor::get);
    }
}
