package at.control.shiftcontrol.shift.service;

import java.util.UUID;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotPreferenceDto;

import lombok.NonNull;

public interface UserPreferenceService {
    @NonNull
    PositionSlotPreferenceDto getUserPreference(@NonNull UUID userId, long shiftId);
}
