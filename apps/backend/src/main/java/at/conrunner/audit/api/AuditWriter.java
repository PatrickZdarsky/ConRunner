package at.conrunner.audit.api;

/** Writes in the caller's existing transaction; failures abort the business operation. */
public interface AuditWriter {
    void record(AuditEntry entry);
}
