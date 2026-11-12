package at.conrunner.shift.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import at.conrunner.shift.lib.type.TradeStatus;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "assignment_switch_request")
public class AssignmentSwitchRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "offering_assignment_id", nullable = false)
    private Assignment offeringAssignment;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "requested_assignment_id", nullable = false)
    private Assignment requestedAssignment;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus status;

    @NotNull
    @Column(nullable = false)
    private Instant createdAt;

    @Override
    public String toString() {
        return "AssignmentSwitchRequest{id=%d, , offeringAssignmentId=%d, requestedAssignmentId=%d, status=%s, createdAt=%s}"
            .formatted(id, offeringAssignment.getId(), requestedAssignment.getId(), status, createdAt);
    }
}
