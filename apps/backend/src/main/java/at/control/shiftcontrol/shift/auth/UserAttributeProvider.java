package at.control.shiftcontrol.shift.auth;

import java.util.Collection;
import java.util.Set;

import lombok.NonNull;

public interface UserAttributeProvider {
    void invalidateUserCache(String userId);

    void invalidateUserCaches(Collection<String> userId);

    Set<Long> getPlansWhereUserIsVolunteer(@NonNull String userId);

    Set<Long> getPlansWhereUserIsPlanner(@NonNull String userId);

    Set<Long> getPlansWhereUserIsLocked(@NonNull String userId);
}
