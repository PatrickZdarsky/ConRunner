package at.conrunner.communications.api;

/** Enqueues a request in the caller's existing domain transaction. */
public interface NotificationRequests {
    void request(NotificationRequest request);
}
