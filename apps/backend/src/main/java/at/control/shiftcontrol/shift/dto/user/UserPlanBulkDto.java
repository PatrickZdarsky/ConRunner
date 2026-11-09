package at.control.shiftcontrol.shift.dto.user;

import java.util.Collection;

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
    private Collection<String> volunteers;
}
