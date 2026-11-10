package at.control.shiftcontrol.shift.dto.userdirectory;

import java.util.Collection;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalUserLookupRequestDto {
    @NotEmpty
    private Collection<UUID> userIds;
}
