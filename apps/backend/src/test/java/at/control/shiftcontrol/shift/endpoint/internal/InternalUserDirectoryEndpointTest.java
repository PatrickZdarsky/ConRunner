package at.control.shiftcontrol.shift.endpoint.internal;

import at.control.shiftcontrol.identity.ApplicationRole;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserService;
import at.control.shiftcontrol.shift.dto.userdirectory.InternalUserLookupRequestDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InternalUserDirectoryEndpointTest {
    @Test
    void resolvesUuidReferencesAndMapsIdentityUsers() {
        var users = mock(UserService.class);
        var id = UUID.randomUUID();
        var user = User.builder().id(id).username("A name").email("a@example.org")
            .avatarUrl("avatar").applicationRoles(List.of(ApplicationRole.PLATFORM_ADMIN)).build();
        when(users.getUsers(List.of(id))).thenReturn(List.of(user));
        var endpoint = new InternalUserDirectoryEndpoint(users);
        var result = endpoint.getUsers(new InternalUserLookupRequestDto(List.of(id)));
        var account = result.iterator().next();
        assertThat(account.getVolunteer().getId()).isEqualTo(id);
        assertThat(account.getVolunteer().getName()).isEqualTo("A name");
        assertThat(account.getProfile()).isEqualTo("avatar");
        assertThat(account.isPlatformAdmin()).isTrue();
        verify(users).getUsers(List.of(id));
    }

    @Test
    void forwardsSearchAndPreservesPagination() {
        var users = mock(UserService.class);
        when(users.searchUsers(eq(1), eq(2), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 2), 5));
        var result = new InternalUserDirectoryEndpoint(users).searchUsers(1, 2, "Ada");
        assertThat(result.getTotal()).isEqualTo(5);
        assertThat(result.getPages()).isEqualTo(3);
        verify(users).searchUsers(eq(1), eq(2), argThat(search -> "Ada".equals(search.getName())));
    }
}
