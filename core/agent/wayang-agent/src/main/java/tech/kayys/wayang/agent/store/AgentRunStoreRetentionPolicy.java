package tech.kayys.wayang.agent.store;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

/**
 * Retention policy for bounded local agent run-store snapshots.
 *
 * <p>A value of {@code 0} disables the corresponding limit, allowing embedded
 * deployments to keep full history while production CLIs and local daemons can
 * bound disk growth with conservative defaults.</p>
 */
public record AgentRunStoreRetentionPolicy(int maxRuns, int maxEventsPerRun) {

    public static final int DEFAULT_MAX_RUNS = 1_000;
    public static final int DEFAULT_MAX_EVENTS_PER_RUN = 1_000;

    public AgentRunStoreRetentionPolicy {
        maxRuns = Math.max(0, maxRuns);
        maxEventsPerRun = Math.max(0, maxEventsPerRun);
    }

    public static AgentRunStoreRetentionPolicy defaults() {
        return new AgentRunStoreRetentionPolicy(DEFAULT_MAX_RUNS, DEFAULT_MAX_EVENTS_PER_RUN);
    }

    public static AgentRunStoreRetentionPolicy unlimited() {
        return new AgentRunStoreRetentionPolicy(0, 0);
    }

    public static AgentRunStoreRetentionPolicy of(int maxRuns, int maxEventsPerRun) {
        return new AgentRunStoreRetentionPolicy(maxRuns, maxEventsPerRun);
    }

    public static AgentRunStoreRetentionPolicy fromMap(Map<String, Object> values) {
        Map<String, Object> source = values == null ? Map.of() : values;
        Object explicit = retentionValue(source);
        if (unlimitedValue(explicit)) {
            return unlimited();
        }
        Map<String, Object> retention = retentionValues(source);
        if (retention.isEmpty()) {
            return defaults();
        }
        if (unlimitedValues(retention)) {
            return unlimited();
        }
        return of(
                intValue(retention, DEFAULT_MAX_RUNS, "maxRuns", "runLimit"),
                intValue(retention, DEFAULT_MAX_EVENTS_PER_RUN, "maxEventsPerRun", "eventLimit", "timelineLimit"));
    }

    public static Map<String, Object> toMap(AgentRunStoreRetentionPolicy policy) {
        AgentRunStoreRetentionPolicy resolved = policy == null ? defaults() : policy;
        return resolved.toMap();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", mode());
        values.put("maxRuns", maxRuns);
        values.put("maxEventsPerRun", maxEventsPerRun);
        values.put("runsBounded", limitsRuns());
        values.put("eventsPerRunBounded", limitsEventsPerRun());
        values.put("bounded", bounded());
        values.put("unlimited", isUnlimited());
        return SdkMaps.copy(values);
    }

    public String mode() {
        return isUnlimited() ? "unlimited" : "bounded";
    }

    public boolean limitsRuns() {
        return maxRuns > 0;
    }

    public boolean limitsEventsPerRun() {
        return maxEventsPerRun > 0;
    }

    public boolean bounded() {
        return limitsRuns() || limitsEventsPerRun();
    }

    public boolean isUnlimited() {
        return !bounded();
    }

    private static Map<String, Object> retentionValues(Map<String, Object> source) {
        Object nested = retentionValue(source);
        if (nested instanceof Map<?, ?> nestedMap) {
            Map<String, Object> values = new LinkedHashMap<>();
            nestedMap.forEach((key, value) -> values.put(String.valueOf(key), value));
            return values;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        copyIfPresent(source, values, "mode");
        copyIfPresent(source, values, "type");
        copyIfPresent(source, values, "kind");
        copyIfPresent(source, values, "unlimited");
        copyIfPresent(source, values, "disabled");
        copyIfPresent(source, values, "enabled");
        copyIfPresent(source, values, "bounded");
        copyIfPresent(source, values, "maxRuns");
        copyIfPresent(source, values, "runLimit");
        copyIfPresent(source, values, "maxEventsPerRun");
        copyIfPresent(source, values, "eventLimit");
        copyIfPresent(source, values, "timelineLimit");
        return values;
    }

    private static Object retentionValue(Map<String, Object> source) {
        Object nested = source.get("retention");
        if (nested == null) {
            nested = source.get("runRetention");
        }
        if (nested == null) {
            nested = source.get("retentionPolicy");
        }
        return nested;
    }

    private static boolean unlimitedValues(Map<String, Object> values) {
        Object mode = first(values, "mode", "type", "kind");
        if (unlimitedValue(mode)) {
            return true;
        }
        if (booleanValue(values, true, "unlimited", "disabled")) {
            return true;
        }
        if (booleanValue(values, false, "enabled", "bounded")) {
            return true;
        }
        return false;
    }

    private static Object first(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                return source.get(key);
            }
        }
        return null;
    }

    private static boolean unlimitedValue(Object value) {
        if (value instanceof Boolean bool) {
            return !bool;
        }
        String normalized = SdkText.trimToEmpty(String.valueOf(value))
                .replace('-', '_')
                .toLowerCase();
        return switch (normalized) {
            case "0", "false", "none", "off", "disabled", "disable", "unlimited", "unbounded" -> true;
            default -> false;
        };
    }

    private static boolean booleanValue(Map<String, Object> source, boolean expected, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Boolean bool) {
                return bool == expected;
            }
            if (value != null) {
                Boolean parsed = booleanFlag(value);
                return parsed != null && parsed == expected;
            }
        }
        return false;
    }

    private static Boolean booleanFlag(Object value) {
        String normalized = SdkText.trimToEmpty(String.valueOf(value))
                .replace('-', '_')
                .toLowerCase();
        return switch (normalized) {
            case "1", "true", "yes", "y", "on", "enabled", "enable" -> true;
            case "0", "false", "no", "n", "off", "disabled", "disable" -> false;
            default -> null;
        };
    }

    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private static int intValue(Map<String, Object> source, int fallback, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null) {
                try {
                    return Integer.parseInt(SdkText.trimToEmpty(String.valueOf(value)));
                } catch (NumberFormatException e) {
                    return fallback;
                }
            }
        }
        return fallback;
    }
}
