package at.control.shiftcontrol.shift.service.impl;

import at.shiftcontrol.shiftservice.dto.positionslot.PositionSlotPreferenceDto;
import at.shiftcontrol.shiftservice.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {
    @Override
    public @NonNull PositionSlotPreferenceDto getUserPreference(@NonNull String userId, long shiftId) {
        //TODO implement method
        return null;
    }
}
