package at.conrunner.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.conrunner.shift.entity.Role;
import at.conrunner.shift.lib.event.BaseEvent;
import at.conrunner.shift.lib.event.EventType;
import at.conrunner.shift.lib.event.RoutingKeys;
import at.conrunner.shift.lib.event.events.parts.RolePart;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleEvent extends BaseEvent {
    private final RolePart role;

    public RoleEvent(EventType eventType, String routingKey, RolePart role) {
        super(eventType, routingKey);
        this.role = role;
    }

    public static RoleEvent ofInternal(EventType eventType, String routingKey, Role role) {
        return new RoleEvent(eventType, routingKey, RolePart.of(role));
    }

    public static RoleEvent roleCreated(Role role) {
        return ofInternal(EventType.ROLE_CREATED, RoutingKeys.ROLE_CREATED, role)
            .withDescription("New role created: " + role.getName());
    }

    public static RoleEvent roleUpdated(Role role) {
        return ofInternal(EventType.ROLE_UPDATED,
            RoutingKeys.format(RoutingKeys.ROLE_UPDATED,
            Map.of("roleId", String.valueOf(role.getId()))), role)
            .withDescription("Role updated: " + role.getName());

    }

    public static RoleEvent roleDeleted(Role role) {
        return ofInternal(EventType.ROLE_DELETED,
            RoutingKeys.format(RoutingKeys.ROLE_DELETED,
            Map.of("roleId", String.valueOf(role.getId()))), role)
            .withDescription("Role deleted: " + role.getName());
    }
}

