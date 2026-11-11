package at.control.shiftcontrol.communications.request;

import at.control.shiftcontrol.communications.api.NotificationContext;
import at.control.shiftcontrol.communications.api.NotificationTypes;
import at.control.shiftcontrol.communications.preference.NotificationLanguageService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationRequestWorkerTest {
    @Test
    void rendersEachRecipientInTheirChosenLanguage() {
        var processing = mock(NotificationRequestProcessingService.class);
        var languages = mock(NotificationLanguageService.class);
        var request = new NotificationRequestProcessingService.ClaimedRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            NotificationTypes.ROLE_ASSIGNED, NotificationContext.encode("42", "Sanität", null), 1, true, true);
        when(processing.claim(anyInt(), any())).thenReturn(List.of(request));
        when(languages.languageFor(request.recipientId())).thenReturn(java.util.Locale.GERMAN);
        new NotificationRequestWorker(processing, languages).process();
        verify(processing).completeWithHistory(request, "Rolle zugewiesen",
            "Dir wurde die Rolle „Sanität“ zugewiesen.", "/events/42");
    }
    @Test
    void distinguishesBulkAdditionsAndRemovals() {
        assertThat(NotificationTemplates.render(NotificationTypes.ROLES_CHANGED,
            NotificationContext.decode(NotificationContext.encode("42", "", "added"))).body())
            .isEqualTo("You were assigned new roles.");
        assertThat(NotificationTemplates.render(NotificationTypes.MEMBERSHIP_CHANGED,
            NotificationContext.decode(NotificationContext.encode("42", "", "removed"))).body())
            .isEqualTo("You were removed from plans.");
    }
    @Test
    void rendersContextAndPlanLinkFromQueuedRequest() {
        var processing = mock(NotificationRequestProcessingService.class);
        var request = new NotificationRequestProcessingService.ClaimedRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            NotificationTypes.ASSIGNMENT_JOIN_REQUESTED,
            NotificationContext.encode("42:7", "Reception", null), 1, false, true);
        when(processing.claim(anyInt(), any())).thenReturn(List.of(request));
        new NotificationRequestWorker(processing, mock(NotificationLanguageService.class)).process();
        verify(processing).completeWithHistory(request, "Join requested", "A volunteer requested to join slot 'Reception'.",
            "/events/42/plans?planId=7&mode=assignments&status=REQUEST_FOR_ASSIGNMENT");
    }
    @Test
    void rendersTradeLinkFromValidatedEventIdentifier() {
        var processing = mock(NotificationRequestProcessingService.class);
        var request = new NotificationRequestProcessingService.ClaimedRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "shift.trade.requested", "42", 1, false, true);
        when(processing.claim(anyInt(), any())).thenReturn(List.of(request));
        new NotificationRequestWorker(processing, mock(NotificationLanguageService.class)).process();
        verify(processing).completeWithHistory(request, "Trade requested", "A volunteer requested a trade with you.", "/events/42");
    }

    @Test
    void retriesInvalidContextWithoutWritingHistory() {
        var processing = mock(NotificationRequestProcessingService.class);
        var request = new NotificationRequestProcessingService.ClaimedRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "shift.trade.requested", "//other-host", 1, false, true);
        when(processing.claim(anyInt(), any())).thenReturn(List.of(request));
        new NotificationRequestWorker(processing, mock(NotificationLanguageService.class)).process();
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
        new NotificationRequestWorker(processing, mock(NotificationLanguageService.class)).process();
        verify(processing).completeWithHistory(request, "Trust alert", "A new trust alert requires review.", "/trust");
    }
}
