package at.conrunner.shift.entity;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
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

import at.conrunner.shift.lib.type.LockStatus;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "shift_plan", indexes = @Index(name = "idx_shift_plan_event", columnList = "event_id"))
public class ShiftPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

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
    @Enumerated(EnumType.STRING)
    @Column(name = "lock_status", nullable = false)
    private LockStatus lockStatus;

    @OneToMany(mappedBy = "shiftPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Shift> shifts;

    @ManyToMany(mappedBy = "volunteeringPlans")
    private Collection<Volunteer> planVolunteers;

    @ManyToMany(mappedBy = "planningPlans")
    private Collection<Volunteer> planPlanners;

    @OneToMany(mappedBy = "shiftPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Role> roles;

    @Column(nullable = false)
    private int defaultNoRolePointsPerMinute;

    @Override
    public String toString() {
        return "ShiftPlan{id=%d, event=%d, name='%s', shifts=%s, planVolunteers=%s, planPlanners=%s}"
            .formatted(
                id,
                event.getId(),
                name,
                shifts.stream().map(Shift::getId).toList(),
                planVolunteers.stream().map(Volunteer::getId).toList(),
                planPlanners.stream().map(Volunteer::getId).toList());
    }

    public void addPlanVolunteer(Volunteer volunteer) {
        if (this.planVolunteers == null) {
            this.planVolunteers = new ArrayList<>();
        }
        if (volunteer.getVolunteeringPlans() == null) {
            volunteer.setVolunteeringPlans(new ArrayList<>());
        }

        this.planVolunteers.add(volunteer);
        volunteer.getVolunteeringPlans().add(this);
    }

    public void removePlanVolunteer(Volunteer volunteer) {
        this.planVolunteers.remove(volunteer);
        volunteer.getVolunteeringPlans().remove(this);
    }

    public void addPlanPlanner(Volunteer volunteer) {
        if (this.planPlanners == null) {
            this.planPlanners = new ArrayList<>();
        }
        if (volunteer.getPlanningPlans() == null) {
            volunteer.setPlanningPlans(new ArrayList<>());
        }

        this.planPlanners.add(volunteer);
        volunteer.getPlanningPlans().add(this);
    }

    public void removePlanPlanner(Volunteer volunteer) {
        this.planPlanners.remove(volunteer);
        volunteer.getPlanningPlans().remove(this);
    }
}
