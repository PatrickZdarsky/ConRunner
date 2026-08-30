package at.control.shiftcontrol.eventcatalog.event;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "eventcatalog_event_link")
public class EventLinkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CatalogEventEntity event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventLinkType type;

    @Column(nullable = false, length = 1024)
    private String url;

    EventLinkEntity(CatalogEventEntity event, EventLinkType type, String url) {
        this.event = event;
        this.type = type;
        this.url = url;
    }
}
