package at.conrunner.shift.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSwitchRequestKey {
    private AssignmentKey offering;
    private AssignmentKey requested;

    public static AssignmentSwitchRequestKey of(AssignmentKey offering, AssignmentKey requested) {
        return AssignmentSwitchRequestKey.builder()
                .offering(offering)
                .requested(requested)
                .build();
    }

    public static AssignmentSwitchRequestKey of(Assignment offering, Assignment requested) {
        return new AssignmentSwitchRequestKey(
            AssignmentKey.of(offering),
            AssignmentKey.of(requested)
        );
    }
}
