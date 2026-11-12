package at.conrunner.identity.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import at.conrunner.identity.ApplicationRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "identity_user_account")
public class UserAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserAccountStatus status;

    @Size(max = 255)
    @Column(name = "username", length = 255)
    private String username;

    @Size(max = 320)
    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Size(max = 2048)
    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "identity_user_account_application_role", joinColumns = @JoinColumn(name = "user_account_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "application_role", nullable = false, length = 64)
    private List<ApplicationRole> applicationRoles = new ArrayList<>();

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

    public boolean grantRole(ApplicationRole role) {
        if (applicationRoles == null) {
            applicationRoles = new ArrayList<>();
        }
        if (applicationRoles.contains(role)) {
            return false;
        }
        try {
            applicationRoles.add(role);
        } catch (UnsupportedOperationException _) {
            applicationRoles = new ArrayList<>(applicationRoles);
            applicationRoles.add(role);
        }
        return true;
    }
}
