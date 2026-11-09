package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.event.BaseEvent;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;

@Data
@EqualsAndHashCode(callSuper = true)
public class PretalxApiKeyInvalidEvent extends BaseEvent {
    private final String apiKey;

    public PretalxApiKeyInvalidEvent(String apiKey) {
        super(EventType.PRETALX_API_KEY_INVALID, RoutingKeys.format(RoutingKeys.PRETALX_API_KEY_INVALID, Map.of("apiKey", apiKey)));
        withDescription("Pretalx API key is invalid and has been removed: " + apiKey);
        this.apiKey = apiKey;
    }
}
