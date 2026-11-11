package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.time.Instant;
import java.util.Collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import at.control.shiftcontrol.shift.entity.Shift;

@Data
@AllArgsConstructor
public class ShiftPart {
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
    private Long relatedActivityId;
    @NotNull
    private Collection<PositionSlotPart> positionSlots;
    private Long locationId;

    @NonNull
    public static ShiftPart of(@NonNull Shift shift) {
        return new ShiftPart(
            String.valueOf(shift.getId()),
            shift.getName(),
            shift.getShortDescription(),
            shift.getLongDescription(),
            shift.getStartTime(),
            shift.getEndTime(),
            shift.getRelatedActivity() != null ? shift.getRelatedActivity().getId() : null,
            shift.getSlots() != null ? shift.getSlots().stream().map(PositionSlotPart::of).toList() : null,
            shift.getLocation() != null ? shift.getLocation().getId() : null
        );
    }
}
