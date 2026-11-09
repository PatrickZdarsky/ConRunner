package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.util.Collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import at.control.shiftcontrol.shift.entity.Role;

@Data
@AllArgsConstructor
public class RolePart {
    @NotNull
    private String id;

    @NotNull
    private String shiftPlanId;

    @NotNull
    private String name;

    private String description;

    @NotNull
    private boolean selfAssignable;

    @NonNull
    public static RolePart of(@NonNull Role role) {
        return new RolePart(
            String.valueOf(role.getId()),
            String.valueOf(role.getShiftPlan().getId()),
            role.getName(),
            role.getDescription(),
            role.isSelfAssignable()
        );
    }

    @NonNull
    public static Collection<RolePart> of(@NonNull Collection<Role> roles) {
        return roles.stream().map(RolePart::of).toList();
    }
}

