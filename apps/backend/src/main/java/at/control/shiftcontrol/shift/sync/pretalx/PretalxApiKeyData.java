package at.control.shiftcontrol.shift.sync.pretalx;

import java.util.List;

import lombok.Value;

@Value
public class PretalxApiKeyData {
    String apiKey;
    String pretalxHost;
    List<String> eventSlugs;
}
