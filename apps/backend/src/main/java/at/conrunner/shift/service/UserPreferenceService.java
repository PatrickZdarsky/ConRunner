package at.conrunner.shift.service;

import java.util.UUID;
import at.conrunner.shift.dto.positionslot.PositionSlotPreferenceDto;

import lombok.NonNull;

public interface UserPreferenceService {
    @NonNull
    PositionSlotPreferenceDto getUserPreference(@NonNull UUID userId, long shiftId);
}
