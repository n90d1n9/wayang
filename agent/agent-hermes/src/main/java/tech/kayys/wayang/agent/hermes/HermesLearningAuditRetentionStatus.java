package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operator-facing retention pressure summary for the learning-audit receipt ledger.
 */
public record HermesLearningAuditRetentionStatus(
        String ledgerType,
        boolean bounded,
        int recordCount,
        int maxEntries,
        int remainingEntries,
        int overflowEntries,
        int utilizationPercent,
        boolean nearCapacity,
        boolean atCapacity,
        String status,
        String severity,
        int priority,
        boolean requiresAttention,
        List<String> attention,
        List<String> recommendedActions,
        Map<String, Object> retentionPolicy,
        Map<String, Object> ledgerMetadata) {

    private static final int NEAR_CAPACITY_PERCENT = 80;

    public HermesLearningAuditRetentionStatus {
        ledgerType = HermesText.oneLineOr(ledgerType, "unknown");
        recordCount = Math.max(recordCount, 0);
        maxEntries = Math.max(maxEntries, 0);
        bounded = bounded && maxEntries > 0;
        remainingEntries = bounded ? Math.max(remainingEntries, 0) : 0;
        overflowEntries = bounded ? Math.max(overflowEntries, 0) : 0;
        utilizationPercent = bounded ? Math.max(utilizationPercent, 0) : 0;
        nearCapacity = bounded && nearCapacity;
        atCapacity = bounded && atCapacity;
        status = bounded ? HermesText.oneLineOr(status, "healthy") : "unbounded";
        severity = HermesText.oneLineOr(severity, severity(status));
        priority = Math.max(priority, priority(severity));
        attention = HermesText.distinctOneLineList(attention);
        recommendedActions = HermesText.distinctOneLineList(recommendedActions);
        requiresAttention = bounded
                && (requiresAttention
                        || !attention.isEmpty()
                        || overflowEntries > 0
                        || atCapacity
                        || nearCapacity);
        retentionPolicy = retentionPolicy == null ? Map.of() : Map.copyOf(retentionPolicy);
        ledgerMetadata = ledgerMetadata == null ? Map.of() : Map.copyOf(ledgerMetadata);
    }

    static HermesLearningAuditRetentionStatus fromLedger(HermesLearningPromotionReceiptLedger ledger) {
        if (ledger == null) {
            return fromMetadata(Map.of("ledgerType", "noop", "recordCount", 0));
        }
        try {
            return fromMetadata(ledger.toMetadata());
        } catch (RuntimeException error) {
            return fromMetadata(Map.of(
                    "ledgerType", "unknown",
                    "recordCount", 0,
                    "metadataError", error.getClass().getSimpleName()));
        }
    }

    static HermesLearningAuditRetentionStatus fromMetadata(Map<String, Object> metadata) {
        Map<String, Object> ledgerMetadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        int recordCount = intValue(ledgerMetadata.get("recordCount"));
        int maxEntries = maxEntries(ledgerMetadata);
        boolean bounded = maxEntries > 0;
        int overflowEntries = bounded ? Math.max(recordCount - maxEntries, 0) : 0;
        int remainingEntries = bounded ? Math.max(maxEntries - recordCount, 0) : 0;
        int utilizationPercent = bounded
                ? (int) Math.ceil((recordCount * 100.0d) / maxEntries)
                : 0;
        boolean atCapacity = bounded && recordCount >= maxEntries;
        boolean nearCapacity = bounded && utilizationPercent >= NEAR_CAPACITY_PERCENT;
        String status = status(bounded, overflowEntries, atCapacity, nearCapacity);
        return new HermesLearningAuditRetentionStatus(
                text(ledgerMetadata.get("ledgerType"), "unknown"),
                bounded,
                recordCount,
                maxEntries,
                remainingEntries,
                overflowEntries,
                utilizationPercent,
                nearCapacity,
                atCapacity,
                status,
                severity(status),
                priority(severity(status)),
                requiresAttention(status),
                attention(status, overflowEntries, utilizationPercent),
                recommendedActions(status),
                retentionPolicy(ledgerMetadata, maxEntries),
                ledgerMetadata);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ledgerType", ledgerType);
        values.put("bounded", bounded);
        values.put("recordCount", recordCount);
        values.put("maxEntries", maxEntries);
        values.put("remainingEntries", remainingEntries);
        values.put("overflowEntries", overflowEntries);
        values.put("utilizationPercent", utilizationPercent);
        values.put("nearCapacity", nearCapacity);
        values.put("atCapacity", atCapacity);
        values.put("status", status);
        values.put("severity", severity);
        values.put("priority", priority);
        values.put("requiresAttention", requiresAttention);
        values.put("attention", attention);
        values.put("recommendedActions", recommendedActions);
        values.put("retentionPolicy", retentionPolicy);
        values.put("ledgerMetadata", ledgerMetadata);
        return Map.copyOf(values);
    }

    private static String status(
            boolean bounded,
            int overflowEntries,
            boolean atCapacity,
            boolean nearCapacity) {
        if (!bounded) {
            return "unbounded";
        }
        if (overflowEntries > 0) {
            return "over-capacity";
        }
        if (atCapacity) {
            return "at-capacity";
        }
        return nearCapacity ? "near-capacity" : "healthy";
    }

    private static String severity(String status) {
        return switch (status) {
            case "over-capacity" -> "critical";
            case "near-capacity", "at-capacity" -> "warning";
            default -> "info";
        };
    }

    private static int priority(String severity) {
        return switch (severity) {
            case "critical" -> 3;
            case "warning" -> 2;
            default -> 0;
        };
    }

    private static boolean requiresAttention(String status) {
        return switch (status) {
            case "near-capacity", "at-capacity", "over-capacity" -> true;
            default -> false;
        };
    }

    private static List<String> attention(
            String status,
            int overflowEntries,
            int utilizationPercent) {
        return switch (status) {
            case "over-capacity" -> List.of(
                    "Learning-audit receipt ledger exceeds retention limit by "
                            + overflowEntries
                            + " entries.");
            case "at-capacity" -> List.of(
                    "Learning-audit receipt ledger is at retention capacity; new receipts can evict oldest entries.");
            case "near-capacity" -> List.of(
                    "Learning-audit receipt ledger is at "
                            + utilizationPercent
                            + "% of retention capacity.");
            default -> List.of();
        };
    }

    private static List<String> recommendedActions(String status) {
        List<String> actions = new ArrayList<>();
        switch (status) {
            case "over-capacity" -> {
                actions.add("verify-learning-audit-ledger-pruning");
                actions.add("increase-learning-audit-retention-limit");
                actions.add("archive-learning-audit-receipts");
            }
            case "at-capacity" -> {
                actions.add("increase-learning-audit-retention-limit");
                actions.add("archive-learning-audit-receipts");
            }
            case "near-capacity" -> {
                actions.add("monitor-learning-audit-retention");
                actions.add("plan-learning-audit-retention-capacity");
            }
            default -> {
            }
        }
        return List.copyOf(actions);
    }

    private static Map<String, Object> retentionPolicy(
            Map<String, Object> metadata,
            int maxEntries) {
        Map<String, Object> policy = objectMap(metadata.get("retentionPolicy"));
        if (!policy.isEmpty() || maxEntries < 1) {
            return policy;
        }
        return Map.of(
                "retentionMode", "max-entries",
                "maxEntries", maxEntries);
    }

    private static int maxEntries(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return 0;
        }
        int direct = firstPositive(
                intValue(objectMap(metadata.get("retentionPolicy")).get("maxEntries")),
                intValue(metadata.get("maxRecords")),
                intValue(metadata.get("maxEvents")));
        if (direct > 0) {
            return direct;
        }
        return firstPositive(
                maxEntries(objectMap(metadata.get("primaryLedger"))),
                maxEntries(objectMap(metadata.get("fallbackLedger"))));
    }

    private static int firstPositive(int... values) {
        if (values == null) {
            return 0;
        }
        for (int value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String text(Object value, String fallback) {
        return value == null ? fallback : HermesText.oneLineOr(String.valueOf(value), fallback);
    }

    private static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> {
            if (key != null) {
                values.put(String.valueOf(key), mapValue);
            }
        });
        return Map.copyOf(values);
    }
}
