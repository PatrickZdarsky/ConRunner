package at.conrunner.shift.entity;

import java.time.Instant;
import java.util.Collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
@Table(name = "event")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

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

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<SocialMediaLink> socialMediaLinks;

    @Size(max = 2048)
    @Column(length = 2048)
    private String rewardPointsRedeemUrl;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Location> locations;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Activity> activities;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<ShiftPlan> shiftPlans;

    @Override
    public String toString() {
        return "Event{id=%d, name='%s', startTime=%s, endTime=%s, socialMediaLinks=%s locations=%s, shiftPlans=%s}"
            .formatted(
                id,
                name,
                startTime,
                endTime,
                socialMediaLinks.stream().map(SocialMediaLink::getId).toList(),
                locations.stream().map(Location::getId).toList(),
                shiftPlans.stream().map(ShiftPlan::getId).toList()
            );
    }
}
