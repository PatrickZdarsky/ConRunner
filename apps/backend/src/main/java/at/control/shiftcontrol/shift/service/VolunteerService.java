package at.control.shiftcontrol.shift.service;

import at.control.shiftcontrol.shift.entity.Volunteer;
import lombok.NonNull;

public interface VolunteerService {

    @NonNull Volunteer getOrCreate(@NonNull String userId);
}
