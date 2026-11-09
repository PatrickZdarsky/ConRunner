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
public class UserEventBulkDto {
    @NotNull
    private Collection<String> plans;

    @NotNull
    private Collection<String> volunteers;
}
