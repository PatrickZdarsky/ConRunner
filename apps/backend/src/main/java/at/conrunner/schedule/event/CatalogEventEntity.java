package at.conrunner.schedule.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "eventcatalog_event")
public class CatalogEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "reward_points_redeem_url", length = 2048)
    private String rewardPointsRedeemUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_mode", nullable = false, length = 16)
    private ScheduleSourceMode sourceMode = ScheduleSourceMode.MANAGED;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_state", nullable = false, length = 24)
    private ScheduleOperationState operationState = ScheduleOperationState.PLANNING;

    @Column(nullable = false, length = 64)
    private String timezone = "UTC";

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventLinkEntity> links = new ArrayList<>();

    public void replaceLinks(List<EventLink> newLinks) {
        links.clear();
        newLinks.forEach(link -> links.add(new EventLinkEntity(this, link.type(), link.url())));
    }
}
