package at.control.shiftcontrol.shift.dao;

import java.util.UUID;
import lombok.NonNull;

import at.control.shiftcontrol.shift.entity.PositionSlot;

public interface PositionSlotDao extends BasicDao<PositionSlot, Long> {
    void setPreference(@NonNull UUID volunteerId, long positionSlotId, int preference);

    int getPreference(@NonNull UUID volunteerId, long positionSlotId);
}
