package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Retention policy for file-backed run-store compaction backups.
 *
 * <p>A value of {@code 0} keeps all backups. Production defaults keep a small
 * rollback window while preventing repeated maintenance from growing local
 * storage without bounds.</p>
 */
public record AgentRunStoreBackupRetentionPolicy(int maxBackups) {

    public static final int DEFAULT_MAX_BACKUPS = 5;

    public AgentRunStoreBackupRetentionPolicy {
        maxBackups = Math.max(0, maxBackups);
    }

    public static AgentRunStoreBackupRetentionPolicy defaults() {
        return new AgentRunStoreBackupRetentionPolicy(DEFAULT_MAX_BACKUPS);
    }

    public static AgentRunStoreBackupRetentionPolicy unlimited() {
        return new AgentRunStoreBackupRetentionPolicy(0);
    }

    public static AgentRunStoreBackupRetentionPolicy of(int maxBackups) {
        return new AgentRunStoreBackupRetentionPolicy(maxBackups);
    }

    public static AgentRunStoreBackupRetentionPolicy fromMap(Map<String, Object> values) {
        Map<String, Object> source = values == null ? Map.of() : values;
        Object explicit = backupRetentionValue(source);
        if (unlimitedValue(explicit)) {
            return unlimited();
        }
        Map<String, Object> retention = backupRetentionValues(source);
        if (retention.isEmpty()) {
            return defaults();
        }
        if (unlimitedValues(retention)) {
            return unlimited();
        }
        return of(intValue(retention, DEFAULT_MAX_BACKUPS, "maxBackups", "backupLimit", "limit"));
    }

    public static Map<String, Object> toMap(AgentRunStoreBackupRetentionPolicy policy) {
        AgentRunStoreBackupRetentionPolicy resolved = policy == null ? defaults() : policy;
        return resolved.toMap();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", mode());
        values.put("maxBackups", maxBackups);
        values.put("bounded", bounded());
        values.put("unlimited", isUnlimited());
        return SdkMaps.copy(values);
    }

    public String mode() {
        return isUnlimited() ? "unlimited" : "bounded";
    }

    public boolean bounded() {
        return maxBackups > 0;
    }

    public boolean isUnlimited() {
        return !bounded();
    }

    private static Map<String, Object> backupRetentionValues(Map<String, Object> source) {
        Object nested = backupRetentionValue(source);
        if (nested instanceof Map<?, ?> nestedMap) {
            Map<String, Object> values = new LinkedHashMap<>();
            nestedMap.forEach((key, value) -> values.put(String.valueOf(key), value));
            return values;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        copyIfPresent(source, values, "backupRetentionMode");
        copyIfPresent(source, values, "mode");
        copyIfPresent(source, values, "type");
        copyIfPresent(source, values, "kind");
        copyIfPresent(source, values, "unlimited");
        copyIfPresent(source, values, "disabled");
        copyIfPresent(source, values, "enabled");
        copyIfPresent(source, values, "bounded");
        copyIfPresent(source, values, "maxBackups");
        copyIfPresent(source, values, "backupLimit");
        copyIfPresent(source, values, "limit");
        return values;
    }

    private static Object backupRetentionValue(Map<String, Object> source) {
        Object nested = source.get("backupRetention");
        if (nested == null) {
            nested = source.get("compactionBackupRetention");
        }
        if (nested == null) {
            nested = source.get("backupRetentionPolicy");
        }
        return nested;
    }

    private static boolean unlimitedValues(Map<String, Object> values) {
        Object mode = first(values, "backupRetentionMode", "mode", "type", "kind");
        if (unlimitedValue(mode)) {
            return true;
        }
        if (booleanValue(values, true, "unlimited", "disabled")) {
            return true;
        }
        return booleanValue(values, false, "enabled", "bounded");
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
        if (value == null) {
            return false;
        }
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

    private static int intValue(Map<String, Object> source, int fallback, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            try {
                return Integer.parseInt(SdkText.trimToEmpty(String.valueOf(value)));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }
}
