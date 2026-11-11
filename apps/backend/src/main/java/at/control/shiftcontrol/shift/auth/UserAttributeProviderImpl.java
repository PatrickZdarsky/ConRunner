package at.control.shiftcontrol.shift.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAttributeProviderImpl implements UserAttributeProvider {
    private LoadingCache<UUID, UserAttributes> attributesCache;
    private final VolunteerDao volunteerDao;

    @PostConstruct
    public void init() {
        attributesCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterAccess(java.time.Duration.ofMinutes(20))
            .build((userId) -> volunteerDao.findById(userId)
                .map(volunteer -> new UserAttributes(
                    volunteer.getVolunteeringPlans().stream().map(ShiftPlan::getId).collect(Collectors.toUnmodifiableSet()),
                    volunteer.getPlanningPlans().stream().map(ShiftPlan::getId).collect(Collectors.toUnmodifiableSet()),
                    volunteer.getLockedPlans().stream().map(ShiftPlan::getId).collect(Collectors.toUnmodifiableSet())
                )).orElse(null));
    }

    @Override
    public void invalidateUserCache(UUID userId) {
        attributesCache.invalidate(userId);
    }

    @Override
    public void invalidateUserCaches(Collection<UUID> userId) {
        attributesCache.invalidateAll(userId);
    }

    @Override
    public Set<Long> getPlansWhereUserIsVolunteer(@NonNull UUID userId) {
        return Optional.ofNullable(attributesCache.get(userId)).map(attributes -> attributes.volunteerPlans).orElse(Collections.emptySet());
    }

    @Override
    public Set<Long> getPlansWhereUserIsPlanner(@NonNull UUID userId) {
        return Optional.ofNullable(attributesCache.get(userId)).map(attributes -> attributes.plannerPlans).orElse(Collections.emptySet());
    }

    @Override
    public Set<Long> getPlansWhereUserIsLocked(@NonNull UUID userId) {
        return Optional.ofNullable(attributesCache.get(userId)).map(attributes -> attributes.lockedPlans).orElse(Collections.emptySet());
    }

    @AllArgsConstructor
    private static class UserAttributes {
        Set<Long> volunteerPlans;
        Set<Long> plannerPlans;
        Set<Long> lockedPlans;
    }
}
