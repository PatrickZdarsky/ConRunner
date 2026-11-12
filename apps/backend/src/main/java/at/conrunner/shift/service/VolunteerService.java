package at.conrunner.shift.service;

import java.util.UUID;
import at.conrunner.shift.entity.Volunteer;
import lombok.NonNull;

public interface VolunteerService {

    @NonNull Volunteer getOrCreate(@NonNull UUID userId);
}
