package at.conrunner.shift.entity;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "shift", indexes = {
    @Index(name = "idx_shift_location", columnList = "location_id"),
    @Index(name = "idx_shift_activity", columnList = "activity_id")
})
@Check(constraints = "activity_id IS NULL OR location_id IS NULL")
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_plan_id", nullable = false)
    private ShiftPlan shiftPlan;

    @NotNull
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Size(max = 255)
    @Column()
    private String shortDescription;

    @Size(max = 1024)
    @Column(length = 1024)
    private String longDescription;

    @NotNull
    @Column(nullable = false)
    private Instant startTime;

    @NotNull
    @Column(nullable = false)
    private Instant endTime;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "activity_id")
    private Activity relatedActivity;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<PositionSlot> slots;

    @Column(nullable = false)
    private int bonusRewardPoints;

    public Location getEffectiveLocation() {
        return relatedActivity == null ? location : relatedActivity.getLocation();
    }

    @Override
    public String toString() {
        return "Shift{id=%d, shiftPlan=%d, name='%s', startTime=%s, endTime=%s, location=%s, relatedActivity=%s, slots=%s}"
            .formatted(
                id,
                shiftPlan.getId(),
                name,
                startTime,
                endTime,
                location,
                relatedActivity,
                slots.stream().map(PositionSlot::getId).toList());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Shift shift = (Shift) o;
        return id == shift.id && Objects.equals(shiftPlan, shift.shiftPlan) && Objects.equals(name, shift.name) &&
            Objects.equals(shortDescription, shift.shortDescription) && Objects.equals(longDescription, shift.longDescription) &&
            Objects.equals(startTime, shift.startTime) && Objects.equals(endTime, shift.endTime) &&
            Objects.equals(location, shift.location) && Objects.equals(relatedActivity, shift.relatedActivity) &&
            Objects.equals(slots, shift.slots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shiftPlan, name, shortDescription, longDescription, startTime, endTime, location, relatedActivity, slots);
    }
}
