package at.control.shiftcontrol.shift.service.impl;

import java.util.HashSet;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.service.VolunteerService;

@Service
@RequiredArgsConstructor
public class VolunteerServiceImpl implements VolunteerService {
    private final VolunteerDao volunteerDao;

    private @NonNull Volunteer createVolunteer(@NonNull String userId) {
        var newVolunteer = Volunteer.builder()
            .id(java.util.UUID.fromString(userId))
            .planningPlans(new HashSet<>())
            .volunteeringPlans(new HashSet<>())
            .lockedPlans(new HashSet<>())
            .roles(new HashSet<>())
            .build();
        try {
            return volunteerDao.save(newVolunteer);
        } catch (DataIntegrityViolationException e) {
            // another concurrent request created the same volunteer in the meantime
            return volunteerDao.getById(java.util.UUID.fromString(userId));
        }
    }

    @Override
    public @NonNull Volunteer getOrCreate(@NonNull String userId) {
        return volunteerDao.findById(java.util.UUID.fromString(userId)).orElseGet(() -> createVolunteer(userId));
    }
}
