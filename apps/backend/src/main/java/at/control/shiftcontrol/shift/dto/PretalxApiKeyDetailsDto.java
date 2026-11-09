package at.control.shiftcontrol.shift.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.sync.pretalx.PretalxApiKeyData;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PretalxApiKeyDetailsDto {
    @NotNull
    private String apiKey;
    @NotNull
    private String pretalxHost;
    @NotNull
    private List<String> accessibleEvents;

    public static PretalxApiKeyDetailsDto of(PretalxApiKeyData data) {
        return PretalxApiKeyDetailsDto.builder()
            .apiKey(data.getApiKey())
            .pretalxHost(data.getPretalxHost())
            .accessibleEvents(data.getEventSlugs())
            .build();
    }
}
