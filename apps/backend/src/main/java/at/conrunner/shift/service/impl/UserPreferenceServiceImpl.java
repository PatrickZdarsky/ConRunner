package at.conrunner.shift.service.impl;

import java.util.UUID;
import at.conrunner.shift.dto.positionslot.PositionSlotPreferenceDto;
import at.conrunner.shift.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {
    @Override
    public @NonNull PositionSlotPreferenceDto getUserPreference(@NonNull UUID userId, long shiftId) {
        //TODO implement method
        //This is part of the planned shiftplan autofill, where users could set preference values for shifts
        return null;
    }
}
