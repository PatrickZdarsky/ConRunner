package at.control.shiftcontrol.identity.service.impl;

import at.control.shiftcontrol.identity.ApplicationRole;
import at.control.shiftcontrol.identity.Authorities;
import at.control.shiftcontrol.identity.auth.JwtTokenDecoder;
import at.control.shiftcontrol.identity.entity.ExternalIdentityEntity;
import at.control.shiftcontrol.identity.entity.UserAccountEntity;
import at.control.shiftcontrol.identity.entity.UserAccountStatus;
import at.control.shiftcontrol.identity.repo.ExternalIdentityRepository;
import at.control.shiftcontrol.identity.repo.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationUserSyncServiceImplTest {
    private static final UUID USER_ID = UUID.fromString("12ec9b96-3a88-4ce9-8c80-64899b3b0bcf");

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ExternalIdentityRepository externalIdentityRepository;

    private ApplicationUserSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApplicationUserSyncServiceImpl(userAccountRepository, externalIdentityRepository);
    }

    @Test
    void createsAnActiveAccountAndExternalIdentityForANewSubject() {
        JwtTokenDecoder.DecodedJwtToken token = new JwtTokenDecoder.DecodedJwtToken(
            "https://identity.example", "subject-1", "pat", "pat@example.com", "https://example.com/pat.png");
        when(externalIdentityRepository.findByIssuerAndSubject(token.issuer(), token.subject()))
            .thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccountEntity.class)))
            .thenAnswer(invocation -> {
                UserAccountEntity account = invocation.getArgument(0);
                account.setId(USER_ID);
                return account;
            });

        var applicationUser = service.constructUserForLogin(token);

        ArgumentCaptor<UserAccountEntity> accountCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        UserAccountEntity account = accountCaptor.getValue();

        assertEquals(UserAccountStatus.ACTIVE, account.getStatus());
        assertEquals("pat", account.getPreferredUsername());
        assertEquals("pat", account.getDisplayName());
        assertEquals("pat@example.com", account.getEmail());
        assertEquals("https://example.com/pat.png", account.getAvatarUrl());
        assertNotNull(account.getCreatedAt());
        assertNotNull(account.getUpdatedAt());

        assertEquals(1, account.getExternalIdentities().size());
        ExternalIdentityEntity identity = account.getExternalIdentities().iterator().next();
        assertSame(account, identity.getUserAccount());
        assertEquals(token.issuer(), identity.getIssuer());
        assertEquals(token.subject(), identity.getSubject());

        assertEquals("pat", applicationUser.getUsername());
        assertEquals(USER_ID, applicationUser.getUserId());
        assertTrue(applicationUser.isEnabled());
        assertTrue(applicationUser.getAuthorities().isEmpty());
    }

    @Test
    void updatesChangedClaimsAndUsesPersistedApplicationRoles() {
        Instant previousUpdate = Instant.parse("2026-01-01T00:00:00Z");
        UserAccountEntity account = UserAccountEntity.builder()
            .id(USER_ID)
            .status(UserAccountStatus.ACTIVE)
            .preferredUsername("old-name")
            .displayName("Custom display name")
            .email("old@example.com")
            .avatarUrl("https://example.com/old.png")
            .applicationRoles(List.of(ApplicationRole.PLATFORM_ADMIN))
            .updatedAt(previousUpdate)
            .build();
        ExternalIdentityEntity identity = ExternalIdentityEntity.builder()
            .issuer("https://identity.example")
            .subject("subject-1")
            .userAccount(account)
            .build();
        JwtTokenDecoder.DecodedJwtToken token = new JwtTokenDecoder.DecodedJwtToken(
            identity.getIssuer(), identity.getSubject(), "new-name", " ", "https://example.com/new.png");

        when(externalIdentityRepository.findByIssuerAndSubject(token.issuer(), token.subject()))
            .thenReturn(Optional.of(identity));
        when(userAccountRepository.save(account)).thenReturn(account);

        var applicationUser = service.constructUserForLogin(token);

        verify(userAccountRepository).save(account);
        assertEquals("new-name", account.getPreferredUsername());
        assertEquals("Custom display name", account.getDisplayName());
        assertEquals("old@example.com", account.getEmail());
        assertEquals("https://example.com/new.png", account.getAvatarUrl());
        assertTrue(account.getUpdatedAt().isAfter(previousUpdate));

        assertEquals("new-name", applicationUser.getUsername());
        assertEquals(USER_ID, applicationUser.getUserId());
        assertTrue(applicationUser.isEnabled());
        assertEquals(
            List.of(Authorities.PLATFORM_ADMIN_ROLE),
            applicationUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @Test
    void keepsSuspendedAccountsDisabled() {
        UserAccountEntity account = UserAccountEntity.builder()
            .id(USER_ID)
            .status(UserAccountStatus.SUSPENDED)
            .preferredUsername("pat")
            .build();
        ExternalIdentityEntity identity = ExternalIdentityEntity.builder()
            .issuer("https://identity.example")
            .subject("subject-1")
            .userAccount(account)
            .build();
        JwtTokenDecoder.DecodedJwtToken token = new JwtTokenDecoder.DecodedJwtToken(
            identity.getIssuer(), identity.getSubject(), "pat", null, null);
        when(externalIdentityRepository.findByIssuerAndSubject(token.issuer(), token.subject()))
            .thenReturn(Optional.of(identity));

        var applicationUser = service.constructUserForLogin(token);

        assertFalse(applicationUser.isEnabled());
        assertEquals(USER_ID, applicationUser.getUserId());
    }
}
