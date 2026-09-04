package at.control.shiftcontrol.shift.volunteer;

import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfileEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from VolunteerProfileEntity profile where profile.userId = :userId")
    java.util.Optional<VolunteerProfileEntity> findByIdForUpdate(@Param("userId") UUID userId);
}
