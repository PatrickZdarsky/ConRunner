package at.control.shiftcontrol.shift.volunteer;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfileEntity, UUID> {
}
