package at.conrunner.shift.dto.userprofile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountInfoDto {
    @NotNull
    @Valid
    private VolunteerDto volunteer;

    @NotNull
    private String username;

    @NotNull
    private String email;

    private String profile;

    @NotNull
    private boolean isPlatformAdmin;
}
