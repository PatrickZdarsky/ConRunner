package at.control.shiftcontrol.communications.request;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationRequestWorkerTest {
    @Test
    void rendersTradeLinkFromValidatedEventIdentifier() {
        var processing = mock(NotificationRequestProcessingService.class);
        var request = new NotificationRequestProcessingService.ClaimedRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "shift.trade.requested", "42", 1, false, true);
        when(processing.claim(anyInt(), any())).thenReturn(List.of(request));
        new NotificationRequestWorker(processing).process();
        verify(processing).completeWithHistory(request, "Trade requested", "A volunteer requested a trade with you.", "/events/42");
    }

    @Test
    void retriesInvalidContextWithoutWritingHistory() {
        var processing = mock(NotificationRequestProcessingService.class);
        var request = new NotificationRequestProcessingService.ClaimedRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "shift.trade.requested", "//other-host", 1, false, true);
        when(processing.claim(anyInt(), any())).thenReturn(List.of(request));
        new NotificationRequestWorker(processing).process();
        verify(processing, never()).completeWithHistory(any(), any(), any(), any());
        verify(processing).retry(eq(request.id()), eq(request.leaseId()), any(), eq(5));
    }

    @Test
    void rendersGlobalTrustAlertsWithoutAnEvent() {
        var processing = mock(NotificationRequestProcessingService.class);
        var request = new NotificationRequestProcessingService.ClaimedRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "shift.trust.alert", "global", 1, false, true);
        when(processing.claim(anyInt(), any())).thenReturn(List.of(request));
        new NotificationRequestWorker(processing).process();
        verify(processing).completeWithHistory(request, "Trust alert", "A new trust alert requires review.", "/trust");
    }
}
