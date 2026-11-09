package at.control.shiftcontrol.shift.plan;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanAccessServiceTest {
    @Mock
    private CurrentShiftActor currentShiftActor;

    @Mock
    private PlanMembershipRepository membershipRepository;

    @InjectMocks
    private PlanAccessService access;

    @Test
    void grantsScopeOnlyWhenTheActorHasTheRequiredMembership() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(currentShiftActor.current()).thenReturn(new CurrentShiftActor.ShiftActor(userId, false));
        when(membershipRepository.existsByPlanIdAndUserId(planId, userId)).thenReturn(true);
        when(membershipRepository.existsByPlanIdAndUserIdAndPlannerTrue(planId, userId)).thenReturn(false);

        assertDoesNotThrow(() -> access.requireMember(planId));
        assertThrows(ShiftAccessDeniedException.class, () -> access.requirePlanner(planId));
        verify(membershipRepository).existsByPlanIdAndUserId(planId, userId);
        verify(membershipRepository).existsByPlanIdAndUserIdAndPlannerTrue(planId, userId);
    }

    @Test
    void platformAdministratorIsAnExplicitScopedOverride() {
        when(currentShiftActor.current()).thenReturn(new CurrentShiftActor.ShiftActor(UUID.randomUUID(), true));

        assertDoesNotThrow(() -> access.requireMember(UUID.randomUUID()));
        assertDoesNotThrow(() -> access.requirePlanner(UUID.randomUUID()));
        assertDoesNotThrow(access::requirePlatformAdmin);
        verify(membershipRepository, never()).existsByPlanIdAndUserId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
