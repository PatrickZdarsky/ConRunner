package at.conrunner.shift.lib.event.events;

import java.util.Collection;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.event.BaseEvent;
import at.conrunner.shift.lib.event.EventType;
import at.conrunner.shift.lib.event.RoutingKeys;
import at.conrunner.shift.lib.event.events.parts.AssignmentPart;
import at.conrunner.shift.lib.event.events.parts.VolunteerPart;
import static at.conrunner.shift.lib.event.RoutingKeys.SHIFTPLAN_LEAVE;

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
