package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import at.control.shiftcontrol.shift.entity.Activity;

@Data
@AllArgsConstructor
public class ActivityPart {
    @NotNull
    private final String id;
    @NotNull
    private final String name;
    private final String description;
    @NotNull
    private final Instant startTime;
    @NotNull
    private final Instant endTime;
    private final LocationPart location;
    @NotNull
    private final boolean readOnly;

    @NonNull
    public static ActivityPart of(@NonNull Activity activity) {
        return new ActivityPart(
            String.valueOf(activity.getId()),
            activity.getName(),
            activity.getDescription(),
            activity.getStartTime(),
            activity.getEndTime(),
            activity.getLocation() != null ? new LocationPart(
                String.valueOf(activity.getLocation().getId()),
                activity.getLocation().getName(),
                activity.getLocation().getDescription(),
                activity.getLocation().getUrl(),
                activity.getLocation().isReadOnly()
            ) : null,
            activity.isReadOnly()
        );
    }
}
