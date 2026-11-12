package at.conrunner.shift.service;

import java.util.UUID;
import java.util.Collection;

import at.conrunner.shift.dto.trade.TradeCandidatesDto;
import at.conrunner.shift.dto.trade.TradeCreateDto;
import at.conrunner.shift.dto.trade.TradeDto;

import lombok.NonNull;

public interface AssignmentSwitchRequestService {
    @NonNull TradeDto getTradeById(long id);

    @NonNull Collection<TradeCandidatesDto> getPositionSlotsToOffer(long requestedPositionSlotId, @NonNull UUID currentUserId);

    @NonNull Collection<TradeDto> createTrade(@NonNull TradeCreateDto tradeCreateDto, @NonNull UUID currentUserId);

    @NonNull TradeDto acceptTrade(long id, @NonNull UUID currentUserId);

    @NonNull TradeDto declineTrade(long id, @NonNull UUID currentUserId);

    @NonNull TradeDto cancelTrade(long id, @NonNull UUID currentUserId);
}
