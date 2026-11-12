package at.conrunner.shift.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PretalxApiKeyDto {
    @NotNull
    private String apiKey;
    @NotNull
    private String pretalxHost;
}
