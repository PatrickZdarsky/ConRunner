package at.control.shiftcontrol.shift.dto.event;

import java.time.Instant;
import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    @NotNull
    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String shortDescription;

    @Size(max = 1024)
    private String longDescription;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    @NotNull
    private boolean active;

    @NotNull
    @Valid
    private Collection<SocialMediaLinkDto> socialMediaLinks;

    @Size(max = 2048)
    private String rewardPointsRedeemUrl;
}
