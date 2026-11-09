package at.control.shiftcontrol.shift.entity;

import java.time.Instant;
import java.util.Collection;

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
@Table(name = "activity")
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @NotNull
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;
    @Size(max = 1024)
    @Column(length = 1024)
    private String description;
    @NotNull
    @Column(nullable = false)
    private Instant startTime;
    @NotNull
    @Column(nullable = false)
    private Instant endTime;
    @OneToMany(mappedBy = "relatedActivity")
    private Collection<Shift> shifts;
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;
    @NotNull
    @Column(nullable = false)
    private boolean readOnly;

    @Override
    public String toString() {
        return "Activity{"
            + "id=" + id
            + ", name='" + name + '\''
            + ", shifts=" + (shifts == null ? "[]" : shifts.stream().map(Shift::getId).toList())
            + ", location=" + location
            + '}';
    }
}
