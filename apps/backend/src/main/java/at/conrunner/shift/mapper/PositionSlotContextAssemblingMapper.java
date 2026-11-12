package at.conrunner.shift.mapper;

import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.dto.positionslot.PositionSlotContextDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PositionSlotContextAssemblingMapper {
    private final RewardPointsAssemblingMapper rewardPointsAssemblingMapper;


    public PositionSlotContextDto toContextDto(@NonNull PositionSlot positionSlot) {
        return new PositionSlotContextDto(
            String.valueOf(positionSlot.getId()),
            positionSlot.getName(),
            positionSlot.getDescription(),
            positionSlot.isSkipAutoAssignment(),
            positionSlot.getDesiredVolunteerCount(),
            positionSlot.getRole() == null ? null : RoleMapper.toRoleDto(positionSlot.getRole()),
            rewardPointsAssemblingMapper.toDto(positionSlot)
        );
    }
}
