package at.control.shiftcontrol.shift.entity;

import java.util.Collection;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "volunteer")
public class Volunteer {
    @Id
    @NotNull
    private UUID id;

    @Column(name = "calendar_share_id", unique = true, length = 64)
    private String calendarShareId;

    @ManyToMany
    @JoinTable(
        name = "volunteer_role",
        joinColumns = @JoinColumn(name = "volunteer_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Collection<Role> roles;

    @ManyToMany
    @JoinTable(
        name = "shift_plan_volunteer_volunteering",
        joinColumns = @JoinColumn(name = "volunteer_id"),
        inverseJoinColumns = @JoinColumn(name = "shift_plan_id")
    )
    private Collection<ShiftPlan> volunteeringPlans;

    @ManyToMany
    @JoinTable(
        name = "shift_plan_volunteer_planing",
        joinColumns = @JoinColumn(name = "volunteer_id"),
        inverseJoinColumns = @JoinColumn(name = "shift_plan_id")
    )
    private Collection<ShiftPlan> planningPlans;

    @ManyToMany
    @JoinTable(
        name = "shift_plan_volunteer_locked",
        joinColumns = @JoinColumn(name = "volunteer_id"),
        inverseJoinColumns = @JoinColumn(name = "shift_plan_id")
    )
    private Collection<ShiftPlan> lockedPlans;

    @Override
    public String toString() {
        return "Volunteer{id='%s', roles=%s, volunteeringPlans=%s, planningPlans=%s, lockedPlans=%s}"
            .formatted(
                id,
                roles,
                volunteeringPlans.stream().map(ShiftPlan::getId).toList(),
                planningPlans.stream().map(ShiftPlan::getId).toList(),
                lockedPlans.stream().map(ShiftPlan::getId).toList()
            );
    }
}
