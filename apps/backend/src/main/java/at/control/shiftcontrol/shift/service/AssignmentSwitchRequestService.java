package at.control.shiftcontrol.shift.service;

import java.util.Collection;

import at.control.shiftcontrol.shift.dto.trade.TradeCandidatesDto;
import at.control.shiftcontrol.shift.dto.trade.TradeCreateDto;
import at.control.shiftcontrol.shift.dto.trade.TradeDto;

import lombok.NonNull;

public interface AssignmentSwitchRequestService {
    @NonNull TradeDto getTradeById(long id);

    @NonNull Collection<TradeCandidatesDto> getPositionSlotsToOffer(long requestedPositionSlotId, @NonNull String currentUserId);

    @NonNull Collection<TradeDto> createTrade(@NonNull TradeCreateDto tradeCreateDto, @NonNull String currentUserId);

    @NonNull TradeDto acceptTrade(long id, @NonNull String currentUserId);

    @NonNull TradeDto declineTrade(long id, @NonNull String currentUserId);

    @NonNull TradeDto cancelTrade(long id, @NonNull String currentUserId);
}
