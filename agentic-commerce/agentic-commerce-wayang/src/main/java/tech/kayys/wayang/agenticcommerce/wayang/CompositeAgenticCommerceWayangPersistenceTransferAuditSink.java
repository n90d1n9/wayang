package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Best-effort fan-out sink for persistence transfer audit trails.
 */
public final class CompositeAgenticCommerceWayangPersistenceTransferAuditSink
        implements AgenticCommerceWayangPersistenceTransferAuditSink,
                AgenticCommerceWayangPersistenceTransferAuditReader {

    private final List<AgenticCommerceWayangPersistenceTransferAuditSink> sinks;

    public CompositeAgenticCommerceWayangPersistenceTransferAuditSink(
            List<? extends AgenticCommerceWayangPersistenceTransferAuditSink> sinks) {
        if (sinks == null) {
            this.sinks = List.of();
        } else {
            this.sinks = sinks.stream()
                    .filter(Objects::nonNull)
                    .map(AgenticCommerceWayangPersistenceTransferAuditSink.class::cast)
                    .toList();
        }
    }

    public CompositeAgenticCommerceWayangPersistenceTransferAuditSink(
            AgenticCommerceWayangPersistenceTransferAuditSink... sinks) {
        this(sinks == null ? List.of() : Arrays.asList(sinks));
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditSink> sinks() {
        return sinks;
    }

    @Override
    public void record(AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        if (trail == null) {
            return;
        }
        for (AgenticCommerceWayangPersistenceTransferAuditSink sink : sinks) {
            try {
                sink.record(trail);
            } catch (RuntimeException ignored) {
                // A failing diagnostic/audit target must not block later sinks.
            }
        }
    }

    @Override
    public AgenticCommerceWayangPersistenceTransferAuditPage query(
            AgenticCommerceWayangPersistenceTransferAuditQuery query) {
        for (AgenticCommerceWayangPersistenceTransferAuditSink sink : sinks) {
            try {
                java.util.Optional<AgenticCommerceWayangPersistenceTransferAuditReader> reader =
                        AgenticCommerceWayangPersistenceTransferAuditReader.readableSink(sink);
                if (reader.isPresent()) {
                    return reader.get().query(query);
                }
            } catch (RuntimeException ignored) {
                // A failing reader must not hide later readable audit sinks.
            }
        }
        return AgenticCommerceWayangPersistenceTransferAuditReader.empty().query(query);
    }
}
