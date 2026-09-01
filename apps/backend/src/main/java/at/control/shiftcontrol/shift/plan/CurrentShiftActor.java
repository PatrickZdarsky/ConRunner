package at.control.shiftcontrol.shift.plan;

import java.util.UUID;

/** Authenticated actor information needed by the shift module. */
public interface CurrentShiftActor {
    ShiftActor current();

    record ShiftActor(UUID userId, boolean platformAdmin) {
    }
}
