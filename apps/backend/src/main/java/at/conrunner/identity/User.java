package at.conrunner.identity;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @NotNull
    private UUID id;

    @NotNull
    private String username;

    @NotNull
    private String email;

    private boolean emailVerified;

    private boolean active;

    @NotNull
    private String avatarUrl;

    @Builder.Default
    private List<ApplicationRole> applicationRoles = List.of();
}
