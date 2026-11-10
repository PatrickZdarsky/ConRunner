package at.control.shiftcontrol.shift.event.events;

import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.lib.event.events.TradeEvent;
import at.control.shiftcontrol.shift.lib.event.events.parts.TradePart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class TradeEventTest {

    @Test
    void ofInternal() {
        AssignmentSwitchRequest trade = mock(AssignmentSwitchRequest.class);
        String routingKey = "routingKey";

        TradePart tradePart = mock(TradePart.class);
        try (var tradePartMock = org.mockito.Mockito.mockStatic(TradePart.class)) {
            tradePartMock.when(() -> TradePart.of(trade)).thenReturn(tradePart);

            TradeEvent tradeEvent = TradeEvent.ofInternal(null, routingKey, trade);

            assertEquals(routingKey, tradeEvent.getRoutingKey());
            assertEquals(tradePart, tradeEvent.getTrade());
        }
    }
}

