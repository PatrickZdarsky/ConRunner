package at.control.shiftcontrol.shift.entity;

import java.util.Collection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Table(name = "role")
public class Role {
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
    @Size(max = 1024)
    @Column(length = 1024)
    private String description;
    @Column(nullable = false)
    private boolean selfAssignable;
    @ManyToMany(mappedBy = "roles")
    private Collection<Volunteer> volunteers;

    @Column(nullable = false)
    private int rewardPointsPerMinute;

    @Override
    public String toString() {
        return "Role{" + name + "}";
    }
}
