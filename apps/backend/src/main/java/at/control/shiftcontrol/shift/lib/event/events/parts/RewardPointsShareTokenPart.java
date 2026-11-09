package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.time.Instant;

import at.shiftcontrol.lib.entity.RewardPointsShareToken;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class RewardPointsShareTokenPart {
    private String token;
    private String name;
    private Instant createdAt;

    @NonNull
    public static RewardPointsShareTokenPart of(@NonNull RewardPointsShareToken shareToken) {
        return RewardPointsShareTokenPart.builder()
            .token(shareToken.getToken())
            .name(shareToken.getName())
            .createdAt(shareToken.getCreatedAt())
            .build();
    }
}
