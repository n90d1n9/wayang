package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

/**
 * Cursor and bounded replay rules for A2A task event streams.
 */
record WayangA2aTaskEventCursor(long afterSequence, int limit) {

    WayangA2aTaskEventCursor {
        afterSequence = Math.max(0, afterSequence);
        limit = normalizeLimit(limit);
    }

    static WayangA2aTaskEventCursor of(long afterSequence, int limit) {
        return new WayangA2aTaskEventCursor(afterSequence, limit);
    }

    static WayangA2aTaskEventCursor fromHttpAttributes(Map<String, Object> attributes) {
        return fromAttributes(attributes, "limit", "pageSize");
    }

    static WayangA2aTaskEventCursor fromJsonRpcParams(Map<String, Object> params) {
        return fromAttributes(params, "pageSize", "limit");
    }

    List<WayangA2aTaskEvent> slice(List<WayangA2aTaskEvent> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        return events.stream()
                .filter(event -> event != null && event.sequence() > afterSequence)
                .limit(limit)
                .toList();
    }

    private static WayangA2aTaskEventCursor fromAttributes(Map<String, Object> attributes, String... limitKeys) {
        Map<String, Object> values = WayangA2aMaps.copyMap(attributes);
        return new WayangA2aTaskEventCursor(
                parseLong(values.get("afterSequence"), 0),
                parseInt(first(values, limitKeys), WayangA2aTaskQuery.DEFAULT_LIMIT));
    }

    private static Object first(Map<String, Object> values, String... keys) {
        if (values == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
    }

    private static long parseLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return WayangA2aTaskQuery.DEFAULT_LIMIT;
        }
        return Math.min(limit, WayangA2aTaskQuery.MAX_LIMIT);
    }
}
