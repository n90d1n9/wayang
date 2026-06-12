package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory persistence transfer audit sink for tests and local diagnostics.
 */
public final class InMemoryAgenticCommerceWayangPersistenceTransferAuditSink
        implements AgenticCommerceWayangPersistenceTransferAuditSink,
                AgenticCommerceWayangPersistenceTransferAuditReader {

    public static final int DEFAULT_MAX_TRAILS = 10_000;

    private final int maxTrails;
    private final CopyOnWriteArrayList<AgenticCommerceWayangPersistenceTransferAuditTrail> trails =
            new CopyOnWriteArrayList<>();

    public InMemoryAgenticCommerceWayangPersistenceTransferAuditSink() {
        this(DEFAULT_MAX_TRAILS);
    }

    public InMemoryAgenticCommerceWayangPersistenceTransferAuditSink(int maxTrails) {
        this.maxTrails = maxTrails < 1 ? DEFAULT_MAX_TRAILS : maxTrails;
    }

    @Override
    public synchronized void record(AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        if (trail != null) {
            trails.add(trail);
            trimToCapacity();
        }
    }

    public synchronized List<AgenticCommerceWayangPersistenceTransferAuditTrail> trails() {
        return List.copyOf(trails);
    }

    public synchronized Optional<AgenticCommerceWayangPersistenceTransferAuditTrail> latest() {
        if (trails.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(trails.get(trails.size() - 1));
    }

    @Override
    public synchronized AgenticCommerceWayangPersistenceTransferAuditPage query(
            AgenticCommerceWayangPersistenceTransferAuditQuery query) {
        return AgenticCommerceWayangPersistenceTransferAuditPage.from(trails, query);
    }

    public synchronized void clear() {
        trails.clear();
    }

    private void trimToCapacity() {
        while (trails.size() > maxTrails) {
            trails.remove(0);
        }
    }
}
