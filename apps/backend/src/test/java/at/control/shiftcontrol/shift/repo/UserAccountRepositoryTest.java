package at.control.shiftcontrol.shift.repo;

import at.control.shiftcontrol.shift.entity.ExternalIdentity;
import at.control.shiftcontrol.shift.entity.UserAccount;
import at.control.shiftcontrol.shift.lib.type.UserAccountStatus;
import at.control.shiftcontrol.shift.lib.type.UserProfileSource;
import at.control.shiftcontrol.shift.repo.userdirectory.ExternalIdentityRepository;
import at.control.shiftcontrol.shift.repo.userdirectory.UserAccountRepository;
import at.control.shiftcontrol.shift.config.TestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

@DataJpaTest(properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({TestConfig.class, at.control.shiftcontrol.TestcontainersConfiguration.class})
class UserAccountRepositoryTest {
    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ExternalIdentityRepository externalIdentityRepository;

    @Test
    void saveAndResolveUserAccountByExternalIdentity() {
        UserAccount userAccount = UserAccount.builder()
            .status(UserAccountStatus.ACTIVE)
            .preferredUsername("directory.user")
            .firstName("Directory")
            .lastName("User")
            .displayName("Directory User")
            .email("directory.user@example.com")
            .profile("https://cdn.example.test/profiles/directory-user.png")
            .emailVerified(true)
            .lastLoginAt(Instant.parse("2026-06-16T09:00:00Z"))
            .lastProfileSyncAt(Instant.parse("2026-06-16T09:05:00Z"))
            .lastProfileSyncSource(UserProfileSource.TOKEN_CLAIMS)
            .build();

        userAccount.addExternalIdentity(ExternalIdentity.builder()
            .issuer("https://id.example.test/realms/shiftcontrol")
            .subject("11111111-2222-3333-4444-555555555555")
            .lastSeenAt(Instant.parse("2026-06-16T09:05:00Z"))
            .build());

        UserAccount savedUserAccount = userAccountRepository.save(userAccount);

        ExternalIdentity externalIdentity = externalIdentityRepository.findByIssuerAndSubject(
            "https://id.example.test/realms/shiftcontrol",
            "11111111-2222-3333-4444-555555555555"
        ).orElseThrow();

        Assertions.assertAll(
            () -> Assertions.assertEquals(savedUserAccount.getId(), externalIdentity.getUserAccount().getId()),
            () -> Assertions.assertEquals("https://cdn.example.test/profiles/directory-user.png", externalIdentity.getUserAccount().getProfile()),
            () -> Assertions.assertEquals(UserProfileSource.TOKEN_CLAIMS, externalIdentity.getUserAccount().getLastProfileSyncSource()),
            () -> Assertions.assertEquals(1, externalIdentityRepository.findAllByUserAccountId(savedUserAccount.getId()).size())
        );
    }

    @Test
    void findAllPlatformAdminsWithExternalIdentities_returnsOnlyMarkedAdmins() {
        UserAccount admin = UserAccount.builder()
            .status(UserAccountStatus.ACTIVE)
            .preferredUsername("admin.user")
            .displayName("Admin User")
            .platformAdmin(true)
            .build();
        admin.addExternalIdentity(ExternalIdentity.builder()
            .issuer("https://id.example.test/realms/shiftcontrol")
            .subject("admin-1")
            .lastSeenAt(Instant.parse("2026-06-16T09:05:00Z"))
            .build());

        UserAccount assigned = UserAccount.builder()
            .status(UserAccountStatus.ACTIVE)
            .preferredUsername("assigned.user")
            .displayName("Assigned User")
            .platformAdmin(false)
            .build();
        assigned.addExternalIdentity(ExternalIdentity.builder()
            .issuer("https://id.example.test/realms/shiftcontrol")
            .subject("user-1")
            .lastSeenAt(Instant.parse("2026-06-16T09:05:00Z"))
            .build());

        userAccountRepository.save(admin);
        userAccountRepository.save(assigned);

        var admins = userAccountRepository.findAllPlatformAdminsWithExternalIdentities();

        Assertions.assertAll(
            () -> Assertions.assertEquals(1, admins.size()),
            () -> Assertions.assertTrue(admins.get(0).isPlatformAdmin()),
            () -> Assertions.assertEquals("admin.user", admins.get(0).getPreferredUsername())
        );
    }

    @Test
    void searchUserIds_filtersAndOrdersByProfileFields() {
        userAccountRepository.save(userAccount("bob-subject", "bobby", "Bob", "Builder", "bob@example.com", false));
        userAccountRepository.save(userAccount("alice-subject", "alice", "Alice", "Anderson", "alice@example.com", false));
        userAccountRepository.save(userAccount("nomatch-subject", "uma", "Uma", "Zimmer", "uma@example.com", false));

        var result = userAccountRepository.searchUserIds("%der%", PageRequest.of(0, 2));
        var accounts = userAccountRepository.findAllWithExternalIdentitiesByIdIn(result.getContent());
        var usernamesById = accounts.stream()
            .collect(java.util.stream.Collectors.toMap(UserAccount::getId, UserAccount::getPreferredUsername));

        Assertions.assertAll(
            () -> Assertions.assertEquals(2, result.getTotalElements()),
            () -> Assertions.assertEquals(2, result.getContent().size()),
            () -> Assertions.assertEquals(
                List.of("alice", "bobby"),
                result.getContent().stream()
                    .map(usernamesById::get)
                    .toList()
            )
        );
    }

    @Test
    void searchUserIds_searchesAllPersistedAccounts() {
        userAccountRepository.save(userAccount("admin-subject", "admin.user", "Admin", "User", "admin@example.com", true));
        userAccountRepository.save(userAccount("plain-subject", "plain.user", "Plain", "User", "plain@example.com", false));

        var result = userAccountRepository.searchUserIds("%admin%", PageRequest.of(0, 10));

        Assertions.assertAll(
            () -> Assertions.assertEquals(1, result.getTotalElements()),
            () -> Assertions.assertEquals(1, result.getContent().size())
        );
    }

    private UserAccount userAccount(String subject, String username, String firstName, String lastName, String email, boolean platformAdmin) {
        UserAccount userAccount = UserAccount.builder()
            .status(UserAccountStatus.ACTIVE)
            .preferredUsername(username)
            .firstName(firstName)
            .lastName(lastName)
            .displayName(firstName + " " + lastName)
            .email(email)
            .profile("https://cdn.example.test/profiles/" + subject + ".png")
            .emailVerified(true)
            .platformAdmin(platformAdmin)
            .build();

        userAccount.addExternalIdentity(ExternalIdentity.builder()
            .issuer("https://id.example.test/realms/shiftcontrol")
            .subject(subject)
            .lastSeenAt(Instant.parse("2026-06-16T09:05:00Z"))
            .build());
        return userAccount;
    }
}
