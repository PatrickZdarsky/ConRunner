package at.control.shiftcontrol.shift.dto.invite;

import java.time.Instant;
import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.ShiftPlanInviteType;
import at.control.shiftcontrol.shift.dto.role.RoleDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPlanInviteDto {
    @NotNull
    private String id;

    @NotNull
    private String code;

    @NotNull
    private ShiftPlanInviteType type;

    @NotNull
    @Valid
    private ShiftPlanDto shiftPlanDto;

    @NotNull
    private boolean active;

    private Instant expiresAt;

    @Min(0)
    private Integer maxUses;

    @NotNull
    @Min(0)
    private int usedCount;

    @Valid
    @NotNull
    private Collection<RoleDto> autoAssignedRoles;

    @NotNull
    private Instant createdAt;

    private Instant revokedAt;
}
