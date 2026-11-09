package at.control.shiftcontrol.shift.endpoint.positionslot;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.dto.trade.TradeCandidatesDto;
import at.shiftcontrol.shiftservice.dto.trade.TradeCreateDto;
import at.shiftcontrol.shiftservice.dto.trade.TradeDto;
import at.shiftcontrol.shiftservice.service.AssignmentSwitchRequestService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/trades", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PositionSlotTradeEndpoint {
    private final AssignmentSwitchRequestService assignmentSwitchRequestService;
    private final ApplicationUserProvider userProvider;

    @GetMapping("/{tradeId}")
    @Operation(
        operationId = "getTradeById",
        description = "Get trade by id"
    )
    public TradeDto getTradeById(@PathVariable long tradeId) {
        return assignmentSwitchRequestService.getTradeById(tradeId);
    }

    @GetMapping("/slots-to-offer/{positionSlotId}")
    @Operation(
        operationId = "getPositionSlotsToOffer",
        description = "Get position slots that can be offered in a trade for the given position slot, based on eligible volunteers"
    )
    public Collection<TradeCandidatesDto> getPositionSlotsToOffer(@PathVariable String positionSlotId) {
        return assignmentSwitchRequestService.getPositionSlotsToOffer(
            ConvertUtil.idToLong(positionSlotId),
            userProvider.getCurrentUser().getUserId());
    }

    @PostMapping
    @Operation(
        operationId = "createTrade",
        description = "Create trade request for a specific position slot in a shift"
    )
    public Collection<TradeDto> createTrade(@RequestBody @Valid TradeCreateDto tradeCreateDto) {
        return assignmentSwitchRequestService.createTrade(
            tradeCreateDto,
            userProvider.getCurrentUser().getUserId());
    }

    @PutMapping("/{tradeId}/accept")
    @Operation(
        operationId = "acceptTrade",
        description = "Accept a trade request for a specific position slot in a shift"
    )
    public TradeDto acceptTrade(@PathVariable String tradeId) {
        return assignmentSwitchRequestService.acceptTrade(ConvertUtil.idToLong(tradeId), userProvider.getCurrentUser().getUserId());
    }

    @PutMapping("/{tradeId}/decline")
    @Operation(
        operationId = "declineTrade",
        description = "Decline a trade request for a specific position slot in a shift"
    )
    public TradeDto declineTrade(@PathVariable String tradeId) {
        return assignmentSwitchRequestService.declineTrade(ConvertUtil.idToLong(tradeId), userProvider.getCurrentUser().getUserId());
    }

    @PutMapping("/{tradeId}/cancel")
    @Operation(
        operationId = "cancelTrade",
        description = "Cancel a request for a specific position slot in a shift"
    )
    public void cancelTrade(@PathVariable String tradeId) {
        assignmentSwitchRequestService.cancelTrade(ConvertUtil.idToLong(tradeId), userProvider.getCurrentUser().getUserId());
    }
}
