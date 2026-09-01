package at.control.shiftcontrol.shift.positionslot;

import java.time.Instant;
import java.util.UUID;

import at.control.shiftcontrol.shift.plan.StaffingMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "shift_staffing_shift")
public class StaffingShiftEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "short_description", length = 255)
    private String shortDescription;

    @Column(name = "long_description", length = 1024)
    private String longDescription;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    /** Present only when this is a standalone staffing shift rather than a session-linked one. */
    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "session_id")
    private UUID sessionId;

    /** Null means inherit the plan's staffing mode. */
    @Enumerated(EnumType.STRING)
    @Column(name = "staffing_mode_override", length = 32)
    private StaffingMode staffingModeOverride;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private long version;
}
