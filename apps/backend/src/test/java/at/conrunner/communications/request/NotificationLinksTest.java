package at.conrunner.communications.request;

import at.conrunner.communications.api.NotificationTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class NotificationLinksTest {
    @Test
    void mapsPlannerNotificationsToRelevantPlanViews() {
        assertThat(NotificationLinks.forRequest(NotificationTypes.ASSIGNMENT_JOIN_REQUESTED, "42:7"))
            .isEqualTo("/events/42/plans?planId=7&mode=assignments&status=REQUEST_FOR_ASSIGNMENT");
        assertThat(NotificationLinks.forRequest(NotificationTypes.ASSIGNMENT_LEAVE_REQUESTED, "42:7"))
            .isEqualTo("/events/42/plans?planId=7&mode=assignments&status=AUCTION_REQUEST_FOR_UNASSIGN");
        assertThat(NotificationLinks.forRequest(NotificationTypes.PLAN_VOLUNTEER_JOINED, "42:7"))
            .isEqualTo("/events/42/plans?planId=7&mode=users");
    }

    @Test
    void rejectsUntrustedOrUnexpectedPlanContextAndAcceptsOldQueuedRequests() {
        assertThat(NotificationLinks.forRequest(NotificationTypes.ASSIGNMENT_JOIN_REQUESTED, "42")).isEqualTo("/events/42");
        assertThatIllegalArgumentException().isThrownBy(() -> NotificationLinks.forRequest(NotificationTypes.ASSIGNMENT_JOIN_REQUESTED, "42:7&x=1"));
        assertThatIllegalArgumentException().isThrownBy(() -> NotificationLinks.forRequest(NotificationTypes.TRADE_REQUESTED, "42:7"));
    }
}
