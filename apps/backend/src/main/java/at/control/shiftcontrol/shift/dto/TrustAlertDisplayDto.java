package at.control.shiftcontrol.shift.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.TrustAlertType;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustAlertDisplayDto {
    // this DTO is used to send persisted alerts to the frontend
    @NotNull
    private String id;

    @NotNull
    private VolunteerDto volunteerDto;

    @NotNull
    private TrustAlertType alertType;

    @NotNull
    private String alertTypeDescription;

    @NotNull
    Instant createdAt;
}
