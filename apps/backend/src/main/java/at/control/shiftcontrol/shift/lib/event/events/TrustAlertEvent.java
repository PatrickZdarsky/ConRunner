package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.TrustAlert;
import at.shiftcontrol.lib.event.BaseEvent;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.lib.event.events.parts.TrustAlertPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class TrustAlertEvent extends BaseEvent {
    private final TrustAlertPart trustAlertPart;

    public TrustAlertEvent(EventType eventType, String routingKey, TrustAlertPart trustAlertPart) {
        super(eventType, routingKey);
        this.trustAlertPart = trustAlertPart;
    }

    public static TrustAlertEvent alertReceived(TrustAlert trustAlert) {
        return new TrustAlertEvent(EventType.TRUST_ALERT_RECEIVED,
            RoutingKeys.format(RoutingKeys.TRUST_ALERT_RECEIVED,
                Map.of("alertId", String.valueOf(trustAlert.getId()))
            ), TrustAlertPart.of(trustAlert)
        ).withDescription("Trust alert received with ID: " + trustAlert.getId());
    }
}
