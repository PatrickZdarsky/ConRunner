package at.control.shiftcontrol.shift.dao;

import lombok.NonNull;

import at.shiftcontrol.lib.entity.PositionSlot;

public interface PositionSlotDao extends BasicDao<PositionSlot, Long> {
    void setPreference(@NonNull String volunteerId, long positionSlotId, int preference);

    int getPreference(@NonNull String volunteerId, long positionSlotId);
}
