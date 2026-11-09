package at.control.shiftcontrol.shift.entity;

import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import at.control.shiftcontrol.shift.lib.util.JsonMapConverter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "location")
public class Location {
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
    @Column(nullable = true, length = 1024)
    private String description;
    @Size(max = 1024)
    @Column(nullable = true, length = 1024)
    private String url;
    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "CLOB")
    private Map<String, Object> additionalProperties;
    @NotNull
    @Column(nullable = false)
    private boolean readOnly;


    @Override
    public String toString() {
        return "Location{"
            + "id=" + id
            + ", name='" + name + '\''
            + ", url='" + url + '\''
            + ", additionalProperties=" + additionalProperties
            + '}';
    }
}
