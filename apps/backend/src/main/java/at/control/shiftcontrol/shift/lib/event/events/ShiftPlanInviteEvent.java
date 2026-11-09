package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.ShiftPlanInvite;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.lib.event.events.parts.InvitePart;
import at.shiftcontrol.lib.event.events.parts.ShiftPlanPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShiftPlanInviteEvent extends ShiftPlanEvent {
    private final InvitePart invite;

    public ShiftPlanInviteEvent(EventType eventType, String routingKey, ShiftPlanPart shiftPlan, InvitePart invite) {
        super(eventType, routingKey, shiftPlan);
        this.invite = invite;
    }

    public static ShiftPlanInviteEvent ofInternal(EventType eventType, String routingKey, ShiftPlanInvite invite) {
        return new ShiftPlanInviteEvent(eventType, routingKey, ShiftPlanPart.of(invite.getShiftPlan()), InvitePart.of(invite));
    }

    public static ShiftPlanInviteEvent inviteCreated(ShiftPlanInvite invite) {
        return ofInternal(EventType.SHIFTPLAN_INVITE_CREATED,
            RoutingKeys.format(RoutingKeys.SHIFTPLAN_INVITE_CREATED,
            Map.of("shiftPlanId", String.valueOf(invite.getShiftPlan().getId()),
                "inviteId", String.valueOf(invite.getId()))), invite)
            .withDescription("Shift plan invite created: " + invite.getId());
    }

    public static ShiftPlanInviteEvent inviteDeleted(ShiftPlanInvite invite) {
        return ofInternal(EventType.SHIFTPLAN_INVITE_DELETED,
            RoutingKeys.format(RoutingKeys.SHIFTPLAN_INVITE_DELETED,
            Map.of("shiftPlanId", String.valueOf(invite.getShiftPlan().getId()),
                "inviteId", String.valueOf(invite.getId()))), invite)
            .withDescription("Shift plan invite deleted: " + invite.getId());
    }

    public static ShiftPlanInviteEvent inviteRevoked(ShiftPlanInvite invite) {
        return ofInternal(EventType.SHIFTPLAN_INVITE_REVOKED,
            RoutingKeys.format(RoutingKeys.SHIFTPLAN_INVITE_REVOKED,
            Map.of("shiftPlanId", String.valueOf(invite.getShiftPlan().getId()),
                "inviteId", String.valueOf(invite.getId()))), invite)
            .withDescription("Shift plan invite revoked: " + invite.getId());
    }
}
