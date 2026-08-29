package at.control.shiftcontrol.identity.impl;

import at.control.shiftcontrol.identity.ApplicationRole;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserSearchRequest;
import at.control.shiftcontrol.identity.entity.UserAccountEntity;
import at.control.shiftcontrol.identity.entity.UserAccountStatus;
import at.control.shiftcontrol.identity.repo.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void getUser_resolvesAnInternalUserIdAndMapsTheAccount() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity account = account(userId, "pat", "pat@example.com");
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        User user = service.getUser(userId);

        assertEquals(userId, user.getId());
        assertEquals("pat", user.getUsername());
        assertEquals("pat@example.com", user.getEmail());
        assertEquals("https://example.com/pat.png", user.getAvatarUrl());
        assertEquals(List.of(ApplicationRole.PLATFORM_ADMIN), user.getApplicationRoles());
    }

    @Test
    void getUsers_preservesTheRequestedOrderAndOmitsUnknownUsers() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        UserAccountEntity first = account(firstId, "first", "first@example.com");
        UserAccountEntity second = account(secondId, "second", "second@example.com");
        when(userAccountRepository.findAllById(org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(first, second));

        List<User> users = List.copyOf(service.getUsers(List.of(secondId, missingId, firstId, secondId)));

        assertEquals(List.of(secondId, firstId), users.stream().map(User::getId).toList());
    }

    @Test
    void searchUsers_mapsAccountsInTheOrderOfThePagedIdResult() {
        PageRequest pageable = PageRequest.of(0, 10);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UserAccountEntity first = account(firstId, "first", "first@example.com");
        UserAccountEntity second = account(secondId, "second", "second@example.com");
        when(userAccountRepository.searchUserIds("%exa%", pageable))
            .thenReturn(new PageImpl<>(List.of(secondId, firstId), pageable, 2));
        when(userAccountRepository.findAllWithExternalIdentitiesByIdIn(List.of(secondId, firstId)))
            .thenReturn(List.of(first, second));

        var result = service.searchUsers(0, 10, UserSearchRequest.builder().name(" ExA ").build());

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of("second", "first"), result.getContent().stream().map(User::getUsername).toList());
    }

    private static UserAccountEntity account(UUID id, String username, String email) {
        return UserAccountEntity.builder()
            .id(id)
            .status(UserAccountStatus.ACTIVE)
            .username(username)
            .email(email)
            .avatarUrl("https://example.com/" + username + ".png")
            .applicationRoles(List.of(ApplicationRole.PLATFORM_ADMIN))
            .build();
    }

}
