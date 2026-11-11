package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.control.shiftcontrol.shift.entity.Role;
import at.control.shiftcontrol.shift.lib.event.EventType;
import at.control.shiftcontrol.shift.lib.event.RoutingKeys;
import at.control.shiftcontrol.shift.lib.event.events.parts.RolePart;
import at.control.shiftcontrol.shift.lib.event.events.parts.ShiftPlanRefPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleVolunteerEvent extends RoleEvent {
    private final String volunteerId;
    private final ShiftPlanRefPart shiftPlanRefPart;

    public RoleVolunteerEvent(EventType eventType, String routingKey, RolePart role, String volunteerId, ShiftPlanRefPart shiftPlanRefPart) {
        super(eventType, routingKey, role);
        this.volunteerId = volunteerId;
        this.shiftPlanRefPart = shiftPlanRefPart;
    }

    public static RoleVolunteerEvent ofInternal(EventType eventType, String routingKey, Role role, String volunteerId) {
        return new RoleVolunteerEvent(eventType, routingKey, RolePart.of(role), volunteerId, ShiftPlanRefPart.of(role.getShiftPlan()));
    }

    public static RoleVolunteerEvent roleAssigned(Role role, String volunteerId) {
        return ofInternal(EventType.ROLE_ASSIGNED,
            RoutingKeys.format(RoutingKeys.ROLE_ASSIGNED, Map.of(
                "roleId", String.valueOf(role.getId()),
                "volunteerId", volunteerId)),
            role, volunteerId)
            .withDescription("Role ID " + role.getId() + " assigned to volunteer ID " + volunteerId);
    }

    public static RoleVolunteerEvent roleUnassigned(Role role, String volunteerId) {
        return ofInternal(EventType.ROLE_UNASSIGNED,
            RoutingKeys.format(RoutingKeys.ROLE_UNASSIGNED, Map.of(
                "roleId", String.valueOf(role.getId()),
                "volunteerId", volunteerId)),
            role, volunteerId)
            .withDescription("Role ID " + role.getId() + " unassigned from volunteer ID " + volunteerId);
    }
}
