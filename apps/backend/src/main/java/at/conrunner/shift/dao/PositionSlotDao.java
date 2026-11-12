package at.conrunner.shift.dao;

import java.util.UUID;
import lombok.NonNull;

import at.conrunner.shift.entity.PositionSlot;

public interface PositionSlotDao extends BasicDao<PositionSlot, Long> {
    void setPreference(@NonNull UUID volunteerId, long positionSlotId, int preference);

    int getPreference(@NonNull UUID volunteerId, long positionSlotId);
}
