package at.control.shiftcontrol.shift.volunteer;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Minimal staffing profile; identity owns all account and profile data. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shift_volunteer")
public class VolunteerProfileEntity {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
