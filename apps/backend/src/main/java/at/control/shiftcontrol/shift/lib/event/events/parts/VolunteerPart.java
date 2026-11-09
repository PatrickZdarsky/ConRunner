package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.util.Collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import at.shiftcontrol.lib.entity.Volunteer;

@Data
@AllArgsConstructor
public class VolunteerPart {
    @NotNull
    private String id;

    @NotNull
    private Collection<String> planningPlans;

    @NotNull
    private Collection<String> lockedPlans;

    @NotNull
    private Collection<String> volunteeringPlans;

    @NotNull
    private Collection<String> roles;

    @NonNull
    public static VolunteerPart of(@NonNull Volunteer volunteer) {
        return new VolunteerPart(
            volunteer.getId(),
            volunteer.getPlanningPlans().stream().map(shiftPlan -> String.valueOf(shiftPlan.getId())).toList(),
            volunteer.getLockedPlans().stream().map(shiftPlan -> String.valueOf(shiftPlan.getId())).toList(),
            volunteer.getVolunteeringPlans().stream().map(shiftPlan -> String.valueOf(shiftPlan.getId())).toList(),
            volunteer.getRoles().stream().map(role -> String.valueOf(role.getId())).toList()
        );
    }

    @NonNull
    public static Collection<VolunteerPart> of(@NonNull Collection<Volunteer> volunteer) {
        return volunteer.stream().map(VolunteerPart::of).toList();
    }
}
