package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.Assignment;
import at.shiftcontrol.lib.event.BaseEvent;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.lib.event.events.parts.AssignmentPart;

/**
 * This event is fired immediately after an assignment switch has been performed.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssignmentSwitchEvent extends BaseEvent {
    private final AssignmentPart requestedAssignment;
    private final AssignmentPart offeringAssignment;

    @JsonCreator
    public AssignmentSwitchEvent(
        @JsonProperty("requestedAssignment") AssignmentPart requestedAssignment,
        @JsonProperty("offeringAssignment") AssignmentPart offeringAssignment) {
        super(EventType.TRADE_REQUEST_COMPLETED, RoutingKeys.format(RoutingKeys.TRADE_REQUEST_COMPLETED,
            Map.of("requestedVolunteerId", requestedAssignment.getVolunteerId(),
                   "offeringVolunteerId", offeringAssignment.getVolunteerId())));
        this.requestedAssignment = requestedAssignment;
        this.offeringAssignment = offeringAssignment;
    }

    public static AssignmentSwitchEvent assignmentSwitched(Assignment requestedAssignment, Assignment offeringAssignment) {
        return new AssignmentSwitchEvent(
            AssignmentPart.of(requestedAssignment),
            AssignmentPart.of(offeringAssignment)
        ).withDescription("Assignment switch completed between volunteers "
            + requestedAssignment.getAssignedVolunteer().getId() + " and "
            + offeringAssignment.getAssignedVolunteer().getId());
    }
}
