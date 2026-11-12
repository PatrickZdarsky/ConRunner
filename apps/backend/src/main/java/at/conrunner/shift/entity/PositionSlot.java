package at.conrunner.shift.entity;

import java.util.Collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "position_slot")
public class PositionSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    private String name;
    private String description;
    @NotNull
    private boolean skipAutoAssignment;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToMany(mappedBy = "positionSlot", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Assignment> assignments;

    @NotNull
    @Column(nullable = false)
    private int desiredVolunteerCount;

    @Column()
    private Integer overrideRewardPoints;

    @Override
    public String toString() {
        return "PositionSlot{id=%d, shift=%d, role=%s, assignments=%s, desiredVolunteerCount=%d, rewardPoints=%d}"
            .formatted(id, shift.getId(), role, assignments, desiredVolunteerCount, overrideRewardPoints);
    }
}
