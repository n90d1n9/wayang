package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral instruction for inspecting the Hermes runtime journal.
 */
public record HermesRuntimeJournalDirective(
        boolean active,
        String operation,
        String target,
        HermesRuntimeEventQuery query,
        String reason) {

    public HermesRuntimeJournalDirective {
        query = query == null ? HermesRuntimeEventQuery.latest() : query;
        operation = HermesDirectiveSupport.clean(operation, active ? "inspect" : "none");
        target = HermesDirectiveSupport.clean(target, target(query));
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "runtime journal inspection requested" : "runtime journal inspection inactive");
    }

    public static HermesRuntimeJournalDirective inspect(HermesRuntimeEventQuery query) {
        HermesRuntimeEventQuery resolved = query == null ? HermesRuntimeEventQuery.latest() : query;
        return new HermesRuntimeJournalDirective(
                true,
                "inspect",
                target(resolved),
                resolved,
                "runtime journal inspection requested");
    }

    public static HermesRuntimeJournalDirective latest(int limit) {
        return inspect(new HermesRuntimeEventQuery("", "", "", "", limit));
    }

    public static HermesRuntimeJournalDirective none() {
        return new HermesRuntimeJournalDirective(
                false,
                "none",
                "",
                HermesRuntimeEventQuery.latest(),
                "runtime journal inspection inactive");
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("target", target);
        metadata.put("reason", reason);
        metadata.put("query", query.toMetadata());
        return Map.copyOf(metadata);
    }

    private static String target(HermesRuntimeEventQuery query) {
        if (!query.requestId().isBlank()) {
            return "request:" + query.requestId();
        }
        if (!query.sessionId().isBlank()) {
            return "session:" + query.sessionId();
        }
        if (!query.userId().isBlank()) {
            return "user:" + query.userId();
        }
        if (!query.tenantId().isBlank()) {
            return "tenant:" + query.tenantId();
        }
        if (!query.type().isBlank()) {
            return "type:" + query.type();
        }
        if (!query.typePrefix().isBlank()) {
            return "type-prefix:" + query.typePrefix();
        }
        if (!query.outcome().isBlank()) {
            return "outcome:" + query.outcome();
        }
        if (query.occurredFrom() != null || query.occurredUntil() != null) {
            return "time-window";
        }
        if (!query.beforeEventId().isBlank()) {
            return "before-event:" + query.beforeEventId();
        }
        if (!query.afterEventId().isBlank()) {
            return "after-event:" + query.afterEventId();
        }
        return "latest";
    }
}
