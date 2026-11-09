package at.control.shiftcontrol.shift.lib.event.events.parts;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import at.shiftcontrol.lib.entity.Location;

@AllArgsConstructor
@Data
public class LocationPart {
    @NotNull
    private String id;
    @NotNull
    private String name;
    private String description;
    private String url;
    @NotNull
    private boolean readOnly;

    @NonNull
    public static LocationPart of(@NonNull Location location) {
        return new LocationPart(
            String.valueOf(location.getId()),
            location.getName(),
            location.getDescription(),
            location.getUrl(),
            location.isReadOnly()
        );
    }
}
