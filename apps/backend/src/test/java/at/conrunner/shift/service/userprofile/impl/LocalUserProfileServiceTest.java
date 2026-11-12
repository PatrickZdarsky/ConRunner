package at.conrunner.shift.service.userprofile.impl;

import at.conrunner.identity.User;
import at.conrunner.platform.exception.ForbiddenException;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.auth.ShiftActor;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.service.userprofile.VolunteerCalendarService;
import at.conrunner.shift.util.SecurityHelper;
import at.conrunner.identity.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalUserProfileServiceTest {
    @Mock private UserService users;
    @Mock private CurrentShiftActor currentActor;
    @Mock private ShiftActor actor;
    @Mock private SecurityHelper security;
    @Mock private VolunteerDao volunteers;
    @Mock private VolunteerCalendarService calendar;
    @InjectMocks private LocalUserProfileService service;

    @Test
    void rejectsAccessToOtherUsersForNonAdmins() {
        when(currentActor.get()).thenReturn(actor);
        when(actor.requireUserId()).thenReturn(UUID.randomUUID());
        when(security.isNotUserAdmin()).thenReturn(true);

        assertThatThrownBy(() -> service.getUserProfile(UUID.randomUUID()))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(users, volunteers, calendar);
    }

    @Test
    void mapsOwnProfileWithoutVolunteerAndIncludesCalendarShareId() {
        UUID userId = UUID.randomUUID();
        when(currentActor.get()).thenReturn(actor);
        when(actor.requireUserId()).thenReturn(userId);
        when(users.getUser(userId)).thenReturn(user(userId));
        when(volunteers.findById(userId)).thenReturn(Optional.empty());
        when(calendar.getOrCreateCalendarShareId(userId)).thenReturn(Optional.of("CALENDAR123"));

        var result = service.getUserProfile(userId);

        assertThat(result.getAccount().getVolunteer().getId()).isEqualTo(userId);
        assertThat(result.getAccount().getUsername()).isEqualTo("alice");
        assertThat(result.getAccount().getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getVolunteeringPlans()).isEmpty();
        assertThat(result.getPlanningPlans()).isEmpty();
        assertThat(result.getCalendarShareId()).isEqualTo("CALENDAR123");
    }

    @Test
    void adminCanViewAnotherProfileWithoutExposingCalendarShareId() {
        UUID userId = UUID.randomUUID();
        when(currentActor.get()).thenReturn(actor);
        when(actor.requireUserId()).thenReturn(UUID.randomUUID());
        when(security.isNotUserAdmin()).thenReturn(false);
        when(users.getUser(userId)).thenReturn(user(userId));
        when(volunteers.findById(userId)).thenReturn(Optional.empty());

        var result = service.getUserProfile(userId);

        assertThat(result.getAccount().getVolunteer().getId()).isEqualTo(userId);
        assertThat(result.getCalendarShareId()).isNull();
        verifyNoInteractions(calendar);
    }

    private static User user(UUID id) {
        return User.builder().id(id).username("alice").email("alice@example.com").build();
    }
}
