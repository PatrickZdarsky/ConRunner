package at.conrunner.shift.lib.event.events.parts;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.conrunner.shift.entity.ShiftPlan;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShiftPlanRefPart {
    private long id;
    private String name;
    private EventRefPart eventRefPart;

    public static ShiftPlanRefPart of(ShiftPlan shiftPlan) {
        return new ShiftPlanRefPart(shiftPlan.getId(), shiftPlan.getName(), EventRefPart.of(shiftPlan.getEvent()));
    }

    public static Collection<ShiftPlanRefPart> of(Collection<ShiftPlan> shiftPlans) {
        return shiftPlans.stream().map(ShiftPlanRefPart::of).toList();
    }
}
