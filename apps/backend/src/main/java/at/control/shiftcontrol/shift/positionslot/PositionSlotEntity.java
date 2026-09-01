package at.control.shiftcontrol.shift.positionslot;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shift_position_slot")
public class PositionSlotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "staffing_shift_id", nullable = false, updatable = false)
    private UUID staffingShiftId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(name = "desired_volunteer_count", nullable = false)
    private int desiredVolunteerCount;

    @Column(name = "required_role_id")
    private UUID requiredRoleId;

    @Column(name = "skip_auto_assignment", nullable = false)
    private boolean skipAutoAssignment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private long version;
}
