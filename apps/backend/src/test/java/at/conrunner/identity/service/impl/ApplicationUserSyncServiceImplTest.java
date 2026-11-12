package at.conrunner.identity.service.impl;

import at.conrunner.identity.ApplicationRole;
import at.conrunner.identity.Authorities;
import at.conrunner.identity.auth.JwtTokenDecoder;
import at.conrunner.identity.config.props.PlatformAdminBootstrap;
import at.conrunner.identity.entity.ExternalIdentityEntity;
import at.conrunner.identity.entity.UserAccountEntity;
import at.conrunner.identity.entity.UserAccountStatus;
import at.conrunner.identity.repo.ExternalIdentityRepository;
import at.conrunner.identity.repo.UserAccountRepository;
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
        service = new ApplicationUserSyncServiceImpl(
            userAccountRepository, externalIdentityRepository, PlatformAdminBootstrap.none());
    }

    @Test
    void emailVerificationFollowsTheCurrentTokenAndAddress() {
        var account = UserAccountEntity.builder().id(USER_ID).status(UserAccountStatus.ACTIVE)
            .username("pat").email("old@example.com").emailVerified(true).build();
        var externalIdentity = ExternalIdentityEntity.builder().userAccount(account).build();
        var token = new JwtTokenDecoder.DecodedJwtToken("https://identity.example", "subject-1", "pat",
            "new@example.com", null, false);
        when(externalIdentityRepository.findByIssuerAndSubject(token.issuer(), token.subject())).thenReturn(Optional.of(externalIdentity));
        when(userAccountRepository.save(any(UserAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.constructUserForLogin(token);
        assertEquals("new@example.com", account.getEmail());
        assertFalse(account.isEmailVerified());
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
        assertEquals("pat", account.getUsername());
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
            .username("old-name")
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
        assertEquals("new-name", account.getUsername());
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
            .username("pat")
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

    @Test
    void grantsPlatformAdminOnlyToTheConfiguredExternalIdentity() {
        JwtTokenDecoder.DecodedJwtToken token = new JwtTokenDecoder.DecodedJwtToken(
            "https://identity.example", "subject-1", "pat", null, null);
        service = new ApplicationUserSyncServiceImpl(
            userAccountRepository,
            externalIdentityRepository,
            new PlatformAdminBootstrap(List.of(new PlatformAdminBootstrap.ExternalIdentity(
                token.issuer(), token.subject())))
        );
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
        assertEquals(List.of(ApplicationRole.PLATFORM_ADMIN), accountCaptor.getValue().getApplicationRoles());
        assertEquals(List.of(Authorities.PLATFORM_ADMIN_ROLE),
            applicationUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }
}
