package at.conrunner.shift.lib.event.events.parts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.conrunner.shift.entity.Event;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventRefPart {
    private long id;
    private String name;

    public static EventRefPart of(Event event) {
        return new EventRefPart(event.getId(), event.getName());
    }
}
