package at.conrunner.shift.service.impl;

import java.util.UUID;
import java.util.HashSet;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.service.VolunteerService;

@Service
@RequiredArgsConstructor
public class VolunteerServiceImpl implements VolunteerService {
    private final VolunteerDao volunteerDao;

    private @NonNull Volunteer createVolunteer(@NonNull UUID userId) {
        var newVolunteer = Volunteer.builder()
            .id(userId)
            .planningPlans(new HashSet<>())
            .volunteeringPlans(new HashSet<>())
            .lockedPlans(new HashSet<>())
            .roles(new HashSet<>())
            .build();
        try {
            return volunteerDao.save(newVolunteer);
        } catch (DataIntegrityViolationException _) {
            // another concurrent request created the same volunteer in the meantime
            return volunteerDao.getById(userId);
        }
    }

    @Override
    public @NonNull Volunteer getOrCreate(@NonNull UUID userId) {
        return volunteerDao.findById(userId).orElseGet(() -> createVolunteer(userId));
    }
}
