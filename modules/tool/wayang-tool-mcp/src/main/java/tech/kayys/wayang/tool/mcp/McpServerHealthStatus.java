package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class McpServerHealthStatus {

    static final String DEGRADED = "DEGRADED";
    static final String DISABLED = "DISABLED";
    static final String HEALTHY = "HEALTHY";
    static final String UNHEALTHY = "UNHEALTHY";
    static final String UNSYNCED = "UNSYNCED";

    private McpServerHealthStatus() {
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    static int rank(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return 0;
        }
        return switch (normalized) {
            case UNHEALTHY -> 4;
            case DEGRADED -> 3;
            case UNSYNCED -> 2;
            case DISABLED, HEALTHY -> 1;
            default -> 0;
        };
    }

    static String higher(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        int leftRank = rank(normalizedLeft);
        int rightRank = rank(normalizedRight);
        if (leftRank == 0 && rightRank == 0) {
            return null;
        }
        return leftRank >= rightRank ? normalizedLeft : normalizedRight;
    }

    static Map<String, Integer> emptyCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(HEALTHY, 0);
        counts.put(DEGRADED, 0);
        counts.put(UNHEALTHY, 0);
        counts.put(UNSYNCED, 0);
        counts.put(DISABLED, 0);
        return counts;
    }
}
