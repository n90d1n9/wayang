package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Filter for task-store listing operations.
 */
public record WayangA2aTaskQuery(String tenant, String contextId, Set<A2aTaskState> states, int limit) {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1_000;

    public WayangA2aTaskQuery {
        tenant = WayangA2aMaps.optional(tenant);
        contextId = WayangA2aMaps.optional(contextId);
        states = normalizeStates(states);
        limit = normalizeLimit(limit);
    }

    public WayangA2aTaskQuery(String contextId, Set<A2aTaskState> states, int limit) {
        this(null, contextId, states, limit);
    }

    public static WayangA2aTaskQuery all() {
        return new WayangA2aTaskQuery(null, null, Set.of(), DEFAULT_LIMIT);
    }

    public static WayangA2aTaskQuery context(String contextId) {
        return new WayangA2aTaskQuery(null, contextId, Set.of(), DEFAULT_LIMIT);
    }

    public boolean matches(A2aTask task) {
        if (task == null) {
            return false;
        }
        if (contextId != null && !contextId.equals(task.contextId())) {
            return false;
        }
        if (!WayangA2aTaskAccess.visibleToTenant(task, Optional.ofNullable(tenant))) {
            return false;
        }
        return states.isEmpty() || states.contains(task.status().state());
    }

    public static WayangA2aTaskQuery fromAttributes(java.util.Map<String, Object> attributes) {
        java.util.Map<String, Object> values = WayangA2aMaps.copyMap(attributes);
        return new WayangA2aTaskQuery(
                WayangA2aTenantHints.fromMap(values).orElse(null),
                WayangA2aMaps.optional(values.get("contextId")),
                parseStates(WayangA2aMaps.firstString(values, "state", "status").orElse(null)),
                parseLimit(values.getOrDefault("limit", values.get("pageSize"))));
    }

    private static Set<A2aTaskState> normalizeStates(Set<A2aTaskState> states) {
        if (states == null || states.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<A2aTaskState> normalized = new LinkedHashSet<>();
        states.stream()
                .filter(state -> state != null && state != A2aTaskState.TASK_STATE_UNSPECIFIED)
                .forEach(normalized::add);
        return Set.copyOf(normalized);
    }

    private static Set<A2aTaskState> parseStates(Object raw) {
        List<String> values = WayangA2aMaps.stringList(raw);
        if (values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<A2aTaskState> states = new LinkedHashSet<>();
        for (String value : values) {
            states.add(A2aTaskState.fromValue(value));
        }
        return Set.copyOf(states);
    }

    private static int parseLimit(Object raw) {
        if (raw == null) {
            return DEFAULT_LIMIT;
        }
        if (raw instanceof Number number) {
            return normalizeLimit(number.intValue());
        }
        try {
            return normalizeLimit(Integer.parseInt(String.valueOf(raw).trim()));
        } catch (NumberFormatException e) {
            return DEFAULT_LIMIT;
        }
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
