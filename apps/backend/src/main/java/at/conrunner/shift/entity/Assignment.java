package at.conrunner.shift.entity;

import java.util.Collection;

import at.conrunner.shift.lib.type.AssignmentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "assignment")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "position_slot_id", nullable = false)
    private PositionSlot positionSlot;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_volunteer_id", nullable = false)
    private Volunteer assignedVolunteer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status;

    @OneToMany(mappedBy = "offeringAssignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<AssignmentSwitchRequest> outgoingSwitchRequests;

    @OneToMany(mappedBy = "requestedAssignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<AssignmentSwitchRequest> incomingSwitchRequests;

    @Column(nullable = false)
    private int acceptedRewardPoints;

    @Override
    public String toString() {
        return "Assignment{id=%s, slotId=%s, volunteerId=%s, status=%s, outgoingSwitchRequests=%s, incomingSwitchRequests=%s}"
            .formatted(id, positionSlot.getId(), assignedVolunteer.getId(), status, outgoingSwitchRequests, incomingSwitchRequests);
    }

    public static Assignment of(PositionSlot positionSlot, Volunteer volunteer, AssignmentStatus status) {
        return Assignment.builder()
            .assignedVolunteer(volunteer)
            .positionSlot(positionSlot)
            .status(status)
            .build();
    }
}
