
package at.control.shiftcontrol.shift.dto.invite;

import java.time.Instant;
import java.util.Collection;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.ShiftPlanInviteType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPlanInviteCreateRequestDto {
    @NotNull
    private ShiftPlanInviteType type;

    private Instant expiresAt;

    @Min(0)
    private Integer maxUses;

    private Collection<String> autoAssignRoleIds;
}
