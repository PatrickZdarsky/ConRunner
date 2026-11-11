package at.control.shiftcontrol.shift.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentPair {
    private long offeringId;
    private long requestedId;

    public static AssignmentPair of(long offeringId, long requestedId) {
        return new AssignmentPair(offeringId, requestedId);
    }
}
