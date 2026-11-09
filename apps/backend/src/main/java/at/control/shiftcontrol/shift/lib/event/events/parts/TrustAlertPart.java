package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import at.shiftcontrol.lib.entity.TrustAlert;
import at.shiftcontrol.lib.type.TrustAlertType;

@AllArgsConstructor
@Data
@Builder
public class TrustAlertPart {
    @NotNull
    private String id;
    @NotNull
    private String volunteerId;
    @NotNull
    private TrustAlertType alertType;
    @NotNull
    Instant createdAt;

    public static TrustAlertPart of(TrustAlert trustAlert) {
        return TrustAlertPart.builder()
            .id(String.valueOf(trustAlert.getId()))
            .volunteerId(trustAlert.getVolunteer().getId())
            .alertType(trustAlert.getAlertType())
            .createdAt(trustAlert.getCreatedAt())
            .build();
    }
}
