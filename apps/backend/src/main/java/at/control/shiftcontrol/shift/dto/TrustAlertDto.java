package at.control.shiftcontrol.shift.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.TrustAlertType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustAlertDto {
    // this DTO is used to send Alerts from TrustService to ShiftService
    @NotNull
    private java.util.UUID userId;

    @NotNull
    private TrustAlertType alertType;

    @NotNull
    Instant createdAt;
}
