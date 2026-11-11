package at.control.shiftcontrol.shift.dto.event;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.SocialMediaLinkType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialMediaLinkDto {
    @NotNull
    @Column(nullable = false, length = 50)
    private SocialMediaLinkType type;

    @NotNull
    @Size(max = 1024)
    private String url;

    public String createKey() {
        return type.name() + "|" + url.trim();
    }
}
