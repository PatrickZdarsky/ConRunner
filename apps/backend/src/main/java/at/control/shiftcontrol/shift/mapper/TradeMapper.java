package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;

import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.dto.trade.TradeDto;
import at.control.shiftcontrol.shift.dto.trade.TradeInfoDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TradeMapper {
    private final AssignmentAssemblingMapper assignmentAssemblingMapper;

    public TradeDto toDto(@NonNull AssignmentSwitchRequest trade) {
        return new TradeDto(
            String.valueOf(trade.getId()),
            assignmentAssemblingMapper.assemble(trade.getOfferingAssignment()),
            assignmentAssemblingMapper.assemble(trade.getRequestedAssignment()),
            trade.getStatus(),
            trade.getCreatedAt()
        );
    }

    public Collection<TradeDto> toDto(@NonNull Collection<AssignmentSwitchRequest> trades) {
        return trades.stream().map(this::toDto).toList();
    }

    public TradeInfoDto toTradeInfoDto(@NonNull AssignmentSwitchRequest trade) {
        return new TradeInfoDto(
            String.valueOf(trade.getId()),
            trade.getOfferingAssignment().getAcceptedRewardPoints(),
            trade.getRequestedAssignment().getAcceptedRewardPoints(),
            assignmentAssemblingMapper.toContextDto(trade.getOfferingAssignment()),
            assignmentAssemblingMapper.toContextDto(trade.getRequestedAssignment()),
            trade.getStatus(),
            trade.getCreatedAt()
        );
    }

    public Collection<TradeInfoDto> toTradeInfoDto(@NonNull Collection<AssignmentSwitchRequest> trades) {
        return trades.stream().map(this::toTradeInfoDto).toList();
    }

    public static AssignmentSwitchRequest shallowCopy(@NonNull AssignmentSwitchRequest trade) {
        return AssignmentSwitchRequest.builder()
            .offeringAssignment(AssignmentAssemblingMapper.shallowCopy(trade.getOfferingAssignment()))
            .requestedAssignment(AssignmentAssemblingMapper.shallowCopy(trade.getRequestedAssignment()))
            .status(trade.getStatus())
            .createdAt(trade.getCreatedAt())
            .build();
    }
}
