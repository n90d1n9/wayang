package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.List;
import java.util.Optional;

/**
 * Reads persistence transfer audit trails from a compatible audit sink.
 */
@FunctionalInterface
public interface AgenticCommerceWayangPersistenceTransferAuditReader {

    AgenticCommerceWayangPersistenceTransferAuditPage query(
            AgenticCommerceWayangPersistenceTransferAuditQuery query);

    static AgenticCommerceWayangPersistenceTransferAuditReader empty() {
        return query -> AgenticCommerceWayangPersistenceTransferAuditPage.from(List.of(), query);
    }

    static AgenticCommerceWayangPersistenceTransferAuditReader forSink(
            AgenticCommerceWayangPersistenceTransferAuditSink sink) {
        return readableSink(sink).orElseGet(AgenticCommerceWayangPersistenceTransferAuditReader::empty);
    }

    static Optional<AgenticCommerceWayangPersistenceTransferAuditReader> readableSink(
            AgenticCommerceWayangPersistenceTransferAuditSink sink) {
        return sink instanceof AgenticCommerceWayangPersistenceTransferAuditReader reader
                ? Optional.of(reader)
                : Optional.empty();
    }
}
