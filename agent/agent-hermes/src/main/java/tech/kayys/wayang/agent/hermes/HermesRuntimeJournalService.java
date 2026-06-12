package tech.kayys.wayang.agent.hermes;

import java.time.Instant;

/**
 * Operational read facade for Hermes runtime journals.
 */
public final class HermesRuntimeJournalService {

    private final HermesRuntimeEventReader reader;

    public HermesRuntimeJournalService(HermesRuntimeEventReader reader) {
        this.reader = reader == null ? HermesRuntimeEventReader.empty() : reader;
    }

    public static HermesRuntimeJournalService fromSink(HermesRuntimeEventSink sink) {
        return new HermesRuntimeJournalService(HermesRuntimeEventReader.forSink(sink));
    }

    public HermesRuntimeEventPage query(HermesRuntimeEventQuery query) {
        return reader.query(query);
    }

    public HermesRuntimeJournalView inspect(HermesRuntimeEventQuery query) {
        HermesRuntimeEventQuery resolved = query == null ? HermesRuntimeEventQuery.latest() : query;
        return HermesRuntimeJournalView.from(resolved, this.query(resolved));
    }

    public HermesRuntimeJournalView inspectLatest(int limit) {
        return inspect(new HermesRuntimeEventQuery("", "", "", "", limit));
    }

    public HermesSessionSnapshot snapshot(HermesRuntimeEventQuery query) {
        return HermesSessionSnapshot.from(inspect(query));
    }

    public HermesSessionSnapshot snapshot(HermesRuntimeJournalView view) {
        return HermesSessionSnapshot.from(view);
    }

    public HermesSessionSnapshot sessionSnapshot(String sessionId, int limit) {
        return snapshot(HermesRuntimeEventQuery.forSession(sessionId, limit));
    }

    public HermesSessionSnapshot requestSnapshot(String requestId, int limit) {
        return snapshot(HermesRuntimeEventQuery.forRequest(requestId, limit));
    }

    public HermesRuntimeEventPage latest(int limit) {
        return query(new HermesRuntimeEventQuery("", "", "", "", limit));
    }

    public HermesRuntimeEventPage failures(int limit) {
        return query(HermesRuntimeEventQuery.failures(limit));
    }

    public HermesRuntimeEventPage learning(int limit) {
        return query(HermesRuntimeEventQuery.learning(limit));
    }

    public HermesRuntimeEventPage request(String requestId, int limit) {
        return query(HermesRuntimeEventQuery.forRequest(requestId, limit));
    }

    public HermesRuntimeEventPage tenant(String tenantId, int limit) {
        return query(HermesRuntimeEventQuery.forTenant(tenantId, limit));
    }

    public HermesRuntimeEventPage session(String sessionId, int limit) {
        return query(HermesRuntimeEventQuery.forSession(sessionId, limit));
    }

    public HermesRuntimeEventPage user(String userId, int limit) {
        return query(HermesRuntimeEventQuery.forUser(userId, limit));
    }

    public HermesRuntimeEventPage timeWindow(Instant occurredFrom, Instant occurredUntil, int limit) {
        return query(HermesRuntimeEventQuery.timeWindow(occurredFrom, occurredUntil, limit));
    }

    public HermesRuntimeJournalSummary summarize() {
        return summarize(HermesRuntimeEventQuery.MAX_LIMIT);
    }

    public HermesRuntimeJournalSummary summarize(int limit) {
        return HermesRuntimeJournalSummary.from(latest(limit));
    }
}
