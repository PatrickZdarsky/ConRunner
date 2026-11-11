package at.control.shiftcontrol.shift.service;

import java.util.UUID;
import at.control.shiftcontrol.shift.entity.Volunteer;
import lombok.NonNull;

public interface VolunteerService {

    @NonNull Volunteer getOrCreate(@NonNull UUID userId);
}
