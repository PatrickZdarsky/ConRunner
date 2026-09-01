package at.control.shiftcontrol.shift.plan;

import java.util.UUID;

public interface PlanAccess {
    CurrentShiftActor.ShiftActor currentActor();

    void requireMember(UUID planId);

    void requirePlanner(UUID planId);

    void requirePlatformAdmin();
}
