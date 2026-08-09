package at.control.shiftcontrol.identity.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_account")
public class UserAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserAccountStatus status;

    @Size(max = 255)
    @Column(name = "preferred_username", length = 255)
    private String preferredUsername;

    @Size(max = 255)
    @Column(name = "first_name", length = 255)
    private String firstName;

    @Size(max = 255)
    @Column(name = "last_name", length = 255)
    private String lastName;

    @Size(max = 255)
    @Column(name = "display_name", length = 255)
    private String displayName;

    @Size(max = 320)
    @Column(name = "email", length = 320)
    private String email;

    @Size(max = 2048)
    @Column(name = "profile", length = 2048)
    private String profile;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "platform_admin", nullable = false)
    private boolean platformAdmin;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_profile_sync_at")
    private Instant lastProfileSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_profile_sync_source", length = 32)
    private UserProfileSource lastProfileSyncSource;

    @NotNull
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @NotNull
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Builder.Default
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<ExternalIdentityEntity> externalIdentities = new ArrayList<>();

    public void addExternalIdentity(ExternalIdentityEntity externalIdentity) {
        externalIdentity.setUserAccount(this);
        externalIdentities.add(externalIdentity);
    }
}
