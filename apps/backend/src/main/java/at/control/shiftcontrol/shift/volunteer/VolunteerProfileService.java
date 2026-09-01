package at.control.shiftcontrol.shift.volunteer;

import java.time.Clock;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VolunteerProfileService {
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final Clock clock;

    public void ensureProfile(UUID userId) {
        if (!volunteerProfileRepository.existsById(userId)) {
            volunteerProfileRepository.save(new VolunteerProfileEntity(userId, clock.instant()));
        }
    }
}
