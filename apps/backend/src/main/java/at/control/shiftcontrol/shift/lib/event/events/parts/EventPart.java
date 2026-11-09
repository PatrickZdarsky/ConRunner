package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import at.shiftcontrol.lib.entity.Event;

@Data
@AllArgsConstructor
public class EventPart {
    @NotNull
    private String id;
    @NotNull
    private String name;
    private String shortDescription;
    private String longDescription;
    @NotNull
    private Instant startTime;
    @NotNull
    private Instant endTime;

    @NonNull
    public static EventPart of(@NonNull Event event) {
        return new EventPart(
            String.valueOf(event.getId()),
            event.getName(),
            event.getShortDescription(),
            event.getLongDescription(),
            event.getStartTime(),
            event.getEndTime()
        );
    }
}
