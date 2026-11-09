package at.control.shiftcontrol.shift.auth;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import lombok.NonNull;

public interface UserAttributeProvider {
    void invalidateUserCache(UUID userId);

    void invalidateUserCaches(Collection<UUID> userId);

    Set<Long> getPlansWhereUserIsVolunteer(@NonNull UUID userId);

    Set<Long> getPlansWhereUserIsPlanner(@NonNull UUID userId);

    Set<Long> getPlansWhereUserIsLocked(@NonNull UUID userId);
}
