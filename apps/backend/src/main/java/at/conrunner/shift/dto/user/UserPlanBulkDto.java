package at.conrunner.shift.dto.user;

import java.util.Collection;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPlanBulkDto {
    @NotNull
    private Collection<String> roles;

    @NotNull
    private Collection<UUID> volunteers;
}
