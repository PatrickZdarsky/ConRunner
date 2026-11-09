package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Collection;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.Assignment;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.event.BaseEvent;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.lib.event.events.parts.AssignmentPart;
import at.shiftcontrol.lib.event.events.parts.VolunteerPart;
import static at.shiftcontrol.lib.event.RoutingKeys.SHIFTPLAN_LEAVE;

@Data
@EqualsAndHashCode(callSuper = true)
public class LeavePlanEvent extends BaseEvent {
    private final VolunteerPart volunteer;
    private final Collection<AssignmentPart> deletedAssignments;

    public LeavePlanEvent(String routingKey, VolunteerPart volunteer, Collection<AssignmentPart> deletedAssignments) {
        super(EventType.SHIFTPLAN_LEAVE, routingKey);
        this.volunteer = volunteer;
        this.deletedAssignments = deletedAssignments;
    }

    public static LeavePlanEvent leavePlan(Volunteer volunteer, String shiftPlanId, Collection<Assignment> deletedAssignments) {
        return new LeavePlanEvent(RoutingKeys.format(SHIFTPLAN_LEAVE, Map.of("shiftPlanId", shiftPlanId)),
            VolunteerPart.of(volunteer),
            AssignmentPart.of(deletedAssignments)
        ).withDescription("Volunteer " + volunteer.getId() + " left shift plan " + shiftPlanId);
    }
}
