package at.control.shiftcontrol.identity.impl;

import at.control.shiftcontrol.identity.ApplicationRole;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserSearchRequest;
import at.control.shiftcontrol.identity.entity.ExternalIdentityEntity;
import at.control.shiftcontrol.identity.entity.UserAccountEntity;
import at.control.shiftcontrol.identity.entity.UserAccountStatus;
import at.control.shiftcontrol.identity.repo.ExternalIdentityRepository;
import at.control.shiftcontrol.identity.repo.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ExternalIdentityRepository externalIdentityRepository;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void getUser_resolvesAnExternalSubjectAndMapsTheAccount() {
        UserAccountEntity account = account(1L, "pat", "pat@example.com");
        when(externalIdentityRepository.findAllBySubjectIn(Set.of("subject-1")))
            .thenReturn(List.of(identity("subject-1", account)));

        User user = service.getUser("subject-1");

        assertEquals("pat", user.getUsername());
        assertEquals("pat@example.com", user.getEmail());
        assertEquals("https://example.com/pat.png", user.getAvatarUrl());
        assertEquals(List.of(ApplicationRole.PLATFORM_ADMIN), user.getApplicationRoles());
    }

    @Test
    void getUsers_preservesTheRequestedOrderAndOmitsUnknownUsers() {
        UserAccountEntity first = account(1L, "first", "first@example.com");
        UserAccountEntity second = account(2L, "second", "second@example.com");
        when(externalIdentityRepository.findAllBySubjectIn(Set.of("second", "missing", "first")))
            .thenReturn(List.of(identity("first", first), identity("second", second)));

        List<User> users = List.copyOf(service.getUsers(List.of("second", "missing", "first", "second")));

        assertEquals(List.of("second", "first"), users.stream().map(User::getUsername).toList());
    }

    @Test
    void searchUsers_mapsAccountsInTheOrderOfThePagedIdResult() {
        PageRequest pageable = PageRequest.of(0, 10);
        UserAccountEntity first = account(1L, "first", "first@example.com");
        UserAccountEntity second = account(2L, "second", "second@example.com");
        when(userAccountRepository.searchUserIds("%exa%", pageable))
            .thenReturn(new PageImpl<>(List.of(2L, 1L), pageable, 2));
        when(userAccountRepository.findAllWithExternalIdentitiesByIdIn(List.of(2L, 1L)))
            .thenReturn(List.of(first, second));

        var result = service.searchUsers(0, 10, UserSearchRequest.builder().name(" ExA ").build());

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of("second", "first"), result.getContent().stream().map(User::getUsername).toList());
    }

    private static UserAccountEntity account(long id, String username, String email) {
        return UserAccountEntity.builder()
            .id(id)
            .status(UserAccountStatus.ACTIVE)
            .preferredUsername(username)
            .email(email)
            .avatarUrl("https://example.com/" + username + ".png")
            .applicationRoles(List.of(ApplicationRole.PLATFORM_ADMIN))
            .build();
    }

    private static ExternalIdentityEntity identity(String subject, UserAccountEntity account) {
        return ExternalIdentityEntity.builder()
            .issuer("https://identity.example")
            .subject(subject)
            .userAccount(account)
            .build();
    }
}
