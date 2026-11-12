package at.conrunner.shift.dto.userprofile;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerDto {
    @NotNull
    private UUID id;

    @NotNull
    private String username;

    @NotNull
    private String name;

    private String profile;
}
