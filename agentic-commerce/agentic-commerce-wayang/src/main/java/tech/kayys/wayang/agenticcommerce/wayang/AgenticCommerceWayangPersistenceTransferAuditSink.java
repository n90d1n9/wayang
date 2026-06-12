package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.List;

/**
 * Receives persistence transfer audit trails for logs, metrics, or stores.
 */
@FunctionalInterface
public interface AgenticCommerceWayangPersistenceTransferAuditSink {

    void record(AgenticCommerceWayangPersistenceTransferAuditTrail trail);

    default void record(AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        if (report != null) {
            record(report.auditTrail());
        }
    }

    default void record(AgenticCommerceWayangPersistenceTransferReport report) {
        if (report != null) {
            record(report.auditTrail());
        }
    }

    default void record(AgenticCommerceWayangPersistenceTransferApplyReport report) {
        if (report != null) {
            record(report.auditTrail());
        }
    }

    static AgenticCommerceWayangPersistenceTransferAuditSink noop() {
        return trail -> {
        };
    }

    static AgenticCommerceWayangPersistenceTransferAuditSink composite(
            List<? extends AgenticCommerceWayangPersistenceTransferAuditSink> sinks) {
        return new CompositeAgenticCommerceWayangPersistenceTransferAuditSink(sinks);
    }

    static AgenticCommerceWayangPersistenceTransferAuditSink composite(
            AgenticCommerceWayangPersistenceTransferAuditSink... sinks) {
        return new CompositeAgenticCommerceWayangPersistenceTransferAuditSink(sinks);
    }
}
