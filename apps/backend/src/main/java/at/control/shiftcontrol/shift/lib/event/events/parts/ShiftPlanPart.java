package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.util.Collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.lib.type.LockStatus;

@AllArgsConstructor
@Data
public class ShiftPlanPart {
    @NotNull
    private String id;

    @NotNull
    private String name;

    private String shortDescription;

    private String longDescription;

    private LockStatus lockStatus;

    private final EventRefPart eventRefPart;

    public static ShiftPlanPart of(ShiftPlan shiftPlan) {
        return new ShiftPlanPart(
            String.valueOf(shiftPlan.getId()),
            shiftPlan.getName(),
            shiftPlan.getShortDescription(),
            shiftPlan.getLongDescription(),
            shiftPlan.getLockStatus(),
            EventRefPart.of(shiftPlan.getEvent())
        );
    }

    @NonNull
    public static Collection<ShiftPlanPart> of(@NonNull Collection<ShiftPlan> volunteer) {
        return volunteer.stream().map(ShiftPlanPart::of).toList();
    }
}
