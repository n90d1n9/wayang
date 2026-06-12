package tech.kayys.wayang.tool.mcp;

import java.util.List;
import java.util.Locale;

final class McpIssueSeverity {

    static final String CRITICAL = "CRITICAL";
    static final String INFO = "INFO";
    static final String WARNING = "WARNING";

    private McpIssueSeverity() {
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    static int rank(String severity) {
        String normalized = normalize(severity);
        if (normalized == null) {
            return 0;
        }
        return switch (normalized) {
            case CRITICAL -> 3;
            case WARNING -> 2;
            case INFO -> 1;
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

    static String highest(List<McpToolServerHealth.IssueDetail> issueDetails) {
        String highest = null;
        for (McpToolServerHealth.IssueDetail detail : issueDetails) {
            highest = higher(highest, detail.severity());
        }
        return highest;
    }
}
